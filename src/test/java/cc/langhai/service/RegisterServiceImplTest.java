package cc.langhai.service;

import cc.langhai.config.system.SystemConfig;
import cc.langhai.domain.Role;
import cc.langhai.domain.User;
import cc.langhai.domain.UserInfo;
import cc.langhai.domain.UserRole;
import cc.langhai.exception.BusinessException;
import cc.langhai.service.impl.RegisterServiceImpl;
import cc.langhai.utils.EmailUtil;
import cc.langhai.utils.IPUtil;
import cc.langhai.utils.StringUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisterServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserInfoService userInfoService;

    @Mock
    private UserService userService;

    @Mock
    private SystemConfig systemConfig;

    @Mock
    private EmailUtil emailUtil;

    @Mock
    private RoleService roleService;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @InjectMocks
    private RegisterServiceImpl registerServiceImpl;

    private static final String TEST_IP = "127.0.0.1";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NICKNAME = "TestNick";
    private static final String TEST_VERIFY_CODE = "123456";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getHeader("x-forwarded-for")).thenReturn(null);
    }

    // ==================== sendEmailCode 测试 ====================

    @Test
    public void testSendEmailCode_Success() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn(null);
        when(valueOperations.get("email:register:" + getToday())).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(null);
        when(emailUtil.send(TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        registerServiceImpl.sendEmailCode(TEST_EMAIL, request);

        verify(valueOperations, times(1)).set(eq("email:register:" + TEST_IP), eq("1"), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations, times(1)).set(eq("email:register:" + getToday()), eq("1"), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations, times(1)).set(eq("email:register:" + TEST_EMAIL), eq(TEST_VERIFY_CODE), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_NullEmail() {
        registerServiceImpl.sendEmailCode(null, request);
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_BlankEmail() {
        registerServiceImpl.sendEmailCode("", request);
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_IPLimit() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("5");
        when(systemConfig.getRegisterIPEmailCount()).thenReturn(5);

        registerServiceImpl.sendEmailCode(TEST_EMAIL, request);
    }

    @Test
    public void testSendEmailCode_IPNotLimit() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("3");
        when(systemConfig.getRegisterIPEmailCount()).thenReturn(5);
        when(valueOperations.get("email:register:" + getToday())).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(null);
        when(emailUtil.send(TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        registerServiceImpl.sendEmailCode(TEST_EMAIL, request);

        verify(valueOperations, times(1)).set(eq("email:register:" + TEST_IP), eq("4"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_DayLimit() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn(null);
        when(valueOperations.get("email:register:" + getToday())).thenReturn("10");
        when(systemConfig.getRegisterDayEmailCount()).thenReturn(10);

        registerServiceImpl.sendEmailCode(TEST_EMAIL, request);
    }

    @Test
    public void testSendEmailCode_DayNotLimit() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn(null);
        when(valueOperations.get("email:register:" + getToday())).thenReturn("5");
        when(systemConfig.getRegisterDayEmailCount()).thenReturn(10);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(null);
        when(emailUtil.send(TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        registerServiceImpl.sendEmailCode(TEST_EMAIL, request);

        verify(valueOperations, times(1)).set(eq("email:register:" + getToday()), eq("6"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_EmailAlreadyUsed() {
        UserInfo userInfo = new UserInfo();
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn(null);
        when(valueOperations.get("email:register:" + getToday())).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(userInfo);

        registerServiceImpl.sendEmailCode(TEST_EMAIL, request);
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_EmailCodeAlreadySent() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn(null);
        when(valueOperations.get("email:register:" + getToday())).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        registerServiceImpl.sendEmailCode(TEST_EMAIL, request);
    }

    // ==================== verifyUsername 测试 ====================

    @Test
    public void testVerifyUsername_Success() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);

        registerServiceImpl.verifyUsername(TEST_USERNAME);
    }

    @Test(expected = BusinessException.class)
    public void testVerifyUsername_NullUsername() {
        registerServiceImpl.verifyUsername(null);
    }

    @Test(expected = BusinessException.class)
    public void testVerifyUsername_BlankUsername() {
        registerServiceImpl.verifyUsername("");
    }

    @Test(expected = BusinessException.class)
    public void testVerifyUsername_UsernameAlreadyExists() {
        User user = new User();
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(user);

        registerServiceImpl.verifyUsername(TEST_USERNAME);
    }

    // ==================== register 测试 ====================

    @Test
    public void testRegister_Success() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("test-secret-12345678");
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);
        when(userService.getUserListByDay(anyString())).thenReturn(new ArrayList<>());
        when(systemConfig.getRegisterDayUserCount()).thenReturn(100);
        when(roleService.list()).thenReturn(new ArrayList<>());
        Role role = new Role();
        role.setId(1L);
        when(roleService.getOne(any())).thenReturn(role);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return null;
        }).when(userService).insertUser(any(User.class));
        doNothing().when(userInfoService).insertUserInfo(any(UserInfo.class));
        when(userRoleService.save(any(UserRole.class))).thenReturn(true);

        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);

        verify(userService, times(1)).insertUser(any(User.class));
        verify(userInfoService, times(1)).insertUserInfo(any(UserInfo.class));
        verify(userRoleService, times(1)).save(any(UserRole.class));
        verify(session, times(1)).setAttribute(eq("user"), any(User.class));
        verify(session, times(1)).setMaxInactiveInterval(eq(60 * 60));
    }

    @Test(expected = BusinessException.class)
    public void testRegister_NullUsername() {
        registerServiceImpl.register(null, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_BlankUsername() {
        registerServiceImpl.register("", TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_NullPassword() {
        registerServiceImpl.register(TEST_USERNAME, null, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_BlankPassword() {
        registerServiceImpl.register(TEST_USERNAME, "", TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_NullNickname() {
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, null, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_BlankNickname() {
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, "", TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_NullEmail() {
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, null, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_BlankEmail() {
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, "", TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_NullVerifyCode() {
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, null, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_BlankVerifyCode() {
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, "", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_UsernameTooShort() {
        registerServiceImpl.register("te", TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_UsernameTooLong() {
        registerServiceImpl.register("testuser123", TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_UsernameNotAlphaNumeric() {
        when(userService.getUserByUsername("test@user")).thenReturn(null);
        registerServiceImpl.register("test@user", TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_UsernameAlreadyExists() {
        User user = new User();
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(user);
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_PasswordTooShort() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        registerServiceImpl.register(TEST_USERNAME, "12345", TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_PasswordTooLong() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        registerServiceImpl.register(TEST_USERNAME, "1234567890123456789", TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_NicknameTooLong() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("test-secret-12345678");
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, "1234567890123", TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_EmailAlreadyUsed() {
        UserInfo userInfo = new UserInfo();
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("test-secret-12345678");
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(userInfo);
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_VerifyCodeWrong() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("test-secret-12345678");
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn("654321");
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_VerifyCodeNull() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("test-secret-12345678");
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(null);
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_DayLimit() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("test-secret-12345678");
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            userList.add(new User());
        }
        when(userService.getUserListByDay(anyString())).thenReturn(userList);
        when(systemConfig.getRegisterDayUserCount()).thenReturn(100);
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
    }

    @Test
    public void testRegister_WithExistingRoles() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("test-secret-12345678");
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);
        when(userService.getUserListByDay(anyString())).thenReturn(new ArrayList<>());
        when(systemConfig.getRegisterDayUserCount()).thenReturn(100);
        List<Role> roleList = new ArrayList<>();
        roleList.add(new Role());
        when(roleService.list()).thenReturn(roleList);
        Role role = new Role();
        role.setId(2L);
        when(roleService.getOne(any())).thenReturn(role);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return null;
        }).when(userService).insertUser(any(User.class));
        doNothing().when(userInfoService).insertUserInfo(any(UserInfo.class));
        when(userRoleService.save(any(UserRole.class))).thenReturn(true);
        registerServiceImpl.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
        verify(roleService, times(1)).getOne(any());
    }

    private String getToday() {
        String format = cn.hutool.core.date.DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        String[] split = format.split(" ");
        return split[0];
    }
}
