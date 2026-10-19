package FoodSeer.service.impl;

import java.util.Map;

import FoodSeer.constant.Roles;
import FoodSeer.entity.DriverStats;
import FoodSeer.repositories.DriverStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import FoodSeer.dto.AuthResponseDto;
import FoodSeer.dto.LoginRequestDto;
import FoodSeer.dto.RegisterRequestDto;
import FoodSeer.entity.User;
import FoodSeer.repositories.UserRepository;
import FoodSeer.security.JwtTokenProvider;
import FoodSeer.service.AuthService;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverStatsRepository driverStatsRepository;

    private PasswordEncoder       passwordEncoder;
    private AuthenticationManager authManager;
    private JwtTokenProvider      jwtService;

    @Override
    public ResponseEntity<Map<String, String>> register ( final RegisterRequestDto req ) {
        // Username checks
        if ( userRepository.existsByUsername( req.username() ) ) {
            logger.error("Username already taken: " + req.username());
            return ResponseEntity.badRequest().body( Map.of( "error", "Username already taken" ) );
        }
        if ( req.username().length() > 50 || req.username().length() < 3 ){
            logger.error("Username must be between 3-50 characters");
            return ResponseEntity.badRequest().body( Map.of( "error", "Username must be between 3-50 characters" ) );
        }
        for(Character c : req.username().toCharArray()){
            if(!Character.isAlphabetic(c) && c != '_' && c != '-'){
                logger.error("Username must only contain letters, -, and _");
                return ResponseEntity.badRequest().body( Map.of( "error", "Username must only contain letters, -, and _" ) );
            }
        }
        
        // Password checks
        if ( req.password().length() < 8 || req.password().length() > 128){
            logger.error("Password must be longer than 8 characters");
            return ResponseEntity.badRequest().body( Map.of( "error", "Password must be longer than 8 characters" ) );
        }

        // Email checks
        if ( req.email().length() > 254 ||  !req.email().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")){
            logger.error("Username must be between 3-50 characters");
            return ResponseEntity.badRequest().body( Map.of( "error", "Username must be between 3-50 characters" ) );
        }
        
        // Role checks - a null role would NPE inside setCorrectRoles below, and an
        // unrecognized role would otherwise silently save a permanently broken account
        // (role="", which fails every hasRole check forever with no error ever shown)
        final String role = setCorrectRoles(req);
        if ( role.isEmpty() ) {
            logger.error("Invalid or missing role: " + req.role());
            return ResponseEntity.badRequest().body( Map.of( "error", "Role must be one of: driver, customer, staff" ) );
        }
        final String hash = passwordEncoder.encode( req.password() );
        final User hashedUser = new User( req, hash, role );
        userRepository.save( hashedUser );
        if(role.equals("ROLE_DRIVER")){
            logger.info("New driver registered: " + req.username());
            DriverStats driverStats = new DriverStats();
            driverStats.setUsername(req.username());
            driverStatsRepository.save(driverStats);
        }
        logger.info("User registered successfully: " + req.username());
        return ResponseEntity.ok( Map.of( "message", "Registered" ) );
    }

    @Override
    public ResponseEntity<AuthResponseDto> login ( final LoginRequestDto req ) {
        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken( req.username(),
                req.password() );
        final Authentication authentication = authManager.authenticate( auth );

        final String token = jwtService.generateToken( authentication );
        return ResponseEntity.ok( new AuthResponseDto( token ) );
    }

    private String setCorrectRoles(final RegisterRequestDto req){
        // a missing role must fall through to "" (handled by the caller above) rather than NPE here
        if(req.role() == null){
            return "";
        }
        if(req.role().toLowerCase().equals("driver")){
            return Roles.ROLE_DRIVER;
        }
        else if(req.role().toLowerCase().equals("customer")){
            return Roles.ROLE_CUSTOMER;
        }
        else if(req.role().toLowerCase().equals("staff")){
            return Roles.ROLE_STAFF;
        }
        return "";
    }
}
