package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Map;

import FoodSeer.constant.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import FoodSeer.dto.AuthResponseDto;
import FoodSeer.dto.LoginRequestDto;
import FoodSeer.dto.RegisterRequestDto;
import FoodSeer.entity.DriverStats;
import FoodSeer.entity.User;
import FoodSeer.repositories.DriverStatsRepository;
import FoodSeer.repositories.UserRepository;
import FoodSeer.security.JwtTokenProvider;
import FoodSeer.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DriverStatsRepository driverStatsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtTokenProvider jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Constructor parameter order matches fields in AuthServiceImpl:
        // userRepository, driverStatsRepository, passwordEncoder, authManager, jwtService
        authService = new AuthServiceImpl(userRepository, driverStatsRepository, passwordEncoder, authManager, jwtService);
    }

    @Test
    void register_whenUsernameExists_returnsBadRequest() {
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.username()).thenReturn("takenUser");
        when(userRepository.existsByUsername("takenUser")).thenReturn(true);

        ResponseEntity<Map<String, String>> resp = authService.register(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().containsKey("error"));
        assertEquals("Username already taken", resp.getBody().get("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenInvalidUsernameChars_returnsBadRequest() {
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.username()).thenReturn("bad$user");
        when(userRepository.existsByUsername("bad$user")).thenReturn(false);

        ResponseEntity<Map<String, String>> resp = authService.register(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().containsKey("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenPasswordTooShort_returnsBadRequest() {
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.username()).thenReturn("validUser");
        when(userRepository.existsByUsername("validUser")).thenReturn(false);
        when(req.password()).thenReturn("a"); // too short per implementation

        ResponseEntity<Map<String, String>> resp = authService.register(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().containsKey("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenPasswordSevenChars_returnsBadRequest() {
        // Boundary check: the stated policy is a minimum of 8 characters, so
        // a 7-character password must still be rejected.
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.username()).thenReturn("validUser");
        when(userRepository.existsByUsername("validUser")).thenReturn(false);
        when(req.password()).thenReturn("abc1234"); // 7 chars

        ResponseEntity<Map<String, String>> resp = authService.register(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().containsKey("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenPasswordEightChars_isAccepted() {
        // Boundary check: an 8-character password meets the stated minimum
        // and should be accepted.
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.username()).thenReturn("validUser");
        when(req.password()).thenReturn("abc12345"); // 8 chars
        when(req.email()).thenReturn("validUser@example.com");
        when(req.role()).thenReturn("customer");
        when(userRepository.existsByUsername("validUser")).thenReturn(false);
        when(passwordEncoder.encode("abc12345")).thenReturn("hashedPwd");

        ResponseEntity<Map<String, String>> resp = authService.register(req);

        assertEquals(200, resp.getStatusCodeValue());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_whenDriverRole_createsUserAndDriverStats() {
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.username()).thenReturn("driverOne");
        when(req.password()).thenReturn("strongPassword");
        when(req.email()).thenReturn("driver1@example.com");
        when(req.role()).thenReturn("driver");
        when(userRepository.existsByUsername("driverOne")).thenReturn(false);
        when(passwordEncoder.encode("strongPassword")).thenReturn("hashedPwd");

        // capture saved user and driverStats
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<DriverStats> statsCaptor = ArgumentCaptor.forClass(DriverStats.class);

        ResponseEntity<Map<String, String>> resp = authService.register(req);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Registered", resp.getBody().get("message"));

        verify(userRepository, times(1)).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("driverOne", saved.getUsername());
        assertEquals("hashedPwd", saved.getPassword());

        verify(driverStatsRepository, times(1)).save(statsCaptor.capture());
        DriverStats ds = statsCaptor.getValue();
        assertEquals("driverOne", ds.getUsername());
    }

    /**
     * An unrecognized role value must be rejected up front, not silently
     * saved as a permanently broken account (role="").
     */
    @Test
    void register_whenRoleUnrecognized_returnsBadRequest_andDoesNotSave() {
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.username()).thenReturn("validUser");
        when(req.password()).thenReturn("abc12345");
        when(req.email()).thenReturn("validUser@example.com");
        when(req.role()).thenReturn("admin"); // not one of driver/customer/staff
        when(userRepository.existsByUsername("validUser")).thenReturn(false);

        ResponseEntity<Map<String, String>> resp = authService.register(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().containsKey("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * A missing role must produce a 400, not an NPE from
     * setCorrectRoles's unguarded req.role().toLowerCase() call.
     */
    @Test
    void register_whenRoleMissing_returnsBadRequest_insteadOfNpe() {
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.username()).thenReturn("validUser");
        when(req.password()).thenReturn("abc12345");
        when(req.email()).thenReturn("validUser@example.com");
        when(req.role()).thenReturn(null);
        when(userRepository.existsByUsername("validUser")).thenReturn(false);

        ResponseEntity<Map<String, String>> resp = authService.register(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().containsKey("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_whenAuthenticationFails_throwsBadCredentialsException() {
        LoginRequestDto req = mock(LoginRequestDto.class);
        when(req.username()).thenReturn("user");
        when(req.password()).thenReturn("pass");

        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
        verify(authManager, times(1)).authenticate(any());
    }

    @Test
    void setCorrectRoles_mapsRolesCorrectly() throws Exception {
        Method m = AuthServiceImpl.class.getDeclaredMethod("setCorrectRoles", RegisterRequestDto.class);
        m.setAccessible(true);

        RegisterRequestDto req = mock(RegisterRequestDto.class);

        when(req.role()).thenReturn("driver");
        Object res = m.invoke(authService, req);
        assertEquals(Roles.ROLE_DRIVER, res);

        when(req.role()).thenReturn("customer");
        res = m.invoke(authService, req);
        assertEquals(Roles.ROLE_CUSTOMER, res);

        when(req.role()).thenReturn("staff");
        res = m.invoke(authService, req);
        assertEquals(Roles.ROLE_STAFF, res);

        // unknown/empty role should return empty string per implementation
        when(req.role()).thenReturn("unknownRole");
        res = m.invoke(authService, req);
        assertEquals("", res);

        // a null role must also return empty string, not NPE on toLowerCase()
        when(req.role()).thenReturn(null);
        res = m.invoke(authService, req);
        assertEquals("", res);
    }

}
