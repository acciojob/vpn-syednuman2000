package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        User user = userRepository2.findById(userId).get();
        countryName = countryName.toUpperCase();
        if(user.getConnected()){
            throw new Exception("Already connected");
        }
        if(user.getOriginalCountry().getCountryName().equals(CountryName.valueOf(countryName))){
            return user;
        }
        if (user.getServiceProviderList().size()==0){
            throw new Exception("Unable to connect");
        }
        List<ServiceProvider>serviceProviderList=user.getServiceProviderList();
        int a=Integer.MAX_VALUE;
        ServiceProvider serviceProvider=null;
        Country country=null;

        Boolean flag=false;

        //traversing each service providers
        for(ServiceProvider serviceProvider1: serviceProviderList){

            List<Country>countryList=serviceProvider1.getCountryList();
            //traversing each country of service provider
            for(Country country1: countryList){
                if(countryName.equalsIgnoreCase(country1.getCountryName().toString()) && a>serviceProvider1.getId()){
                    a=serviceProvider1.getId();
                    serviceProvider=serviceProvider1;
                    country=country1;

                    flag=true;
                }
            }
        }

        if(!flag){
            throw new Exception("Unable to connect");
        }
        //here we have service provider present with given countryname so make a connection
        Connection connection=new Connection();
        connection.setUser(user);
        connection.setServiceProvider(serviceProvider);

        //set the maskedIp as "updatedCountryCode.serviceProviderId.userId"
        String countryCode=country.getCode();
        String mask=countryCode+"."+serviceProvider.getId()+"."+userId;

        user.setMaskedIp(mask);
        user.setConnected(true);

        user.getConnectionList().add(connection);

        serviceProvider.getConnectionList().add(connection);

        userRepository2.save(user);
        serviceProviderRepository2.save(serviceProvider);

        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user = userRepository2.findById(userId).get();
        if(!user.getConnected()){
            throw new Exception("Already disconnected");
        }
        user.setMaskedIp(null);
        user.setConnected(false);

        userRepository2.save(user);

        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User sender = userRepository2.findById(senderId).get();
        User reciver = userRepository2.findById(receiverId).get();

        CountryName reciverCountryName = null;
        if(reciver.getConnected()){
            String reciverCountryCode;
            String[] arr = reciver.getMaskedIp().split("\\.");
            reciverCountryCode = arr[0];
            for(CountryName countryName : CountryName.values()){
                if(countryName.toCode().equals(reciverCountryCode)){
                    reciverCountryName = countryName;
                    break;
                }
            }
        }else{
            reciverCountryName = reciver.getOriginalCountry().getCountryName();
        }

        if(reciverCountryName.equals(sender.getOriginalCountry().getCountryName())){
            return sender;
        }

        try {
            sender = connect(senderId, reciverCountryName.name());
        }catch (Exception e){
            throw new Exception("Cannot establish communication");
        }

        return sender;
    }
}
