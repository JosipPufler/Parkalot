package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.configs.TokenFilter;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.repositories.UserRepo;
import hr.algebra.parkalot.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;

@Service
public class Setuper {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepo userRepository;
    @Autowired
    private JwtService jwtService;
    public String adminToken;
    public User adminUser;
    public String officerToken;
    public User officerUser;
    public String userToken;
    public User user;
    @Autowired
    private TokenFilter tokenFilter;

    public String setupAdmin(){
        adminUser = UserHelper.getUser();
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateJwtToken(adminUser);
        return adminToken;
    }

    public String setupUser(){
        user = UserHelper.getUser();
        user = userRepository.save(user);
        userToken = jwtService.generateJwtToken(user);
        return userToken;
    }

    public String setupOfficer(){
        officerUser = UserHelper.getOfficer();
        officerUser = userRepository.save(officerUser);
        officerToken = jwtService.generateJwtToken(officerUser);
        return officerToken;
    }

    public void cleanUp(){
        officerToken = null;
        userToken = null;
        adminToken = null;
        officerUser = null;
        user = null;
        adminUser = null;
    }
}
