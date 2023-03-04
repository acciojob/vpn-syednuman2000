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
        List<ServiceProvider> serviceProviderList = user.getServiceProviderList();
        int serviceProviderId = Integer.MAX_VALUE;
        for(ServiceProvider serviceProvider : serviceProviderList){
            List<Country> countryList = serviceProvider.getCountryList();
            for(Country country : countryList){
                if(country.getCountryName().equals(CountryName.valueOf(countryName))){
                    if(serviceProvider.getId() < serviceProviderId){
                        serviceProviderId = serviceProvider.getId();
                        break;
                    }
                }
            }
        }
        if(serviceProviderId == Integer.MAX_VALUE){
            throw new Exception("Unable to connect");
        }

        user.setConnected(true);
        user.setMaskedIp(CountryName.valueOf(countryName).toCode()+"."+serviceProviderId+"."+userId);

        userRepository2.save(user);

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
            String arr[] = reciver.getMaskedIp().split(".");
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
