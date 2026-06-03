package cc.langhai.service;

import cc.langhai.config.system.SystemConfig;
import cc.langhai.domain.Role;
import cc.langhai.domain.User;
import cc.langhai.domain.UserInfo;
import cc.langhai.domain.UserRole;
import cc.langhai.exception.BusinessException;
import cc.langhai.response.UserReturnCode;
import cc.langhai.service.impl.RegisterServiceImpl;
import cc.langhai.utils.DateUtil;
import cc.langhai.utils.EmailUtil;
import cc.langhai.utils.IPUtil;
import cc.langhai.utils.StringUtil;
import cn.hutool.crypto.digest.DigestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisterServiceImplTest {

    @InjectMocks
    private RegisterServiceImpl registerService;

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

    private MockedStatic<IPUtil> ipUtilMock;
    private MockedStatic<DateUtil> dateUtilMock;
    private MockedStatic<StringUtil> stringUtilMock;
    private MockedStatic<DigestUtil> digestUtilMock;
    private MockedStatic<UUID> uuidMock;

    private static final String TEST_IP = "127.0.0.1";
    private static final String TEST_DAY = "2024-01-01";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NICKNAME = "TestNick";
    private static final String TEST_VERIFY_CODE = "123456";
    private static final String TEST_SECRET = "1234567890123456";

    @Before
    public void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ipUtilMock = mockStatic(IPUtil.class);
        dateUtilMock = mockStatic(DateUtil.class);
        stringUtilMock = mockStatic(StringUtil.class);
        digestUtilMock = mockStatic(DigestUtil.class);
        uuidMock = mockStatic(UUID.class);

        ipUtilMock.when(() -> IPUtil.getIP(any())).thenReturn(TEST_IP);
        dateUtilMock.when(DateUtil::getNowDay).thenReturn(TEST_DAY);
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(anyString())).thenReturn(true);
        digestUtilMock.when(() -> DigestUtil.md5Hex(anyString())).thenReturn("mockedHash");
        uuidMock.when(UUID::randomUUID).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        when(systemConfig.getRegisterIPEmailCount()).thenReturn(5);
        when(systemConfig.getRegisterDayEmailCount()).thenReturn(10);
        when(systemConfig.getRegisterDayUserCount()).thenReturn(100);
        when(systemConfig.getSecret()).thenReturn(TEST_SECRET);
    }

    @After
    public void tearDown() {
        ipUtilMock.close();
        dateUtilMock.close();
        stringUtilMock.close();
        digestUtilMock.close();
        uuidMock.close();
    }

    // ==================== sendEmailCode tests ====================

    @Test
    public void testSendEmailCode_EmailBlank() {
        try {
            registerService.sendEmailCode("", request);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.REGISTER_EMAIL_NULL_00005, e.getReturnCode());
        }
    }

    @Test
    public void testSendEmailCode_EmailNull() {
        try {
            registerService.sendEmailCode(null, request);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.REGISTER_EMAIL_NULL_00005, e.getReturnCode());
        }
    }

    @Test
    public void testSendEmailCode_IpCountBlank_DayCountBlank_Success() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_DAY)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(null);
        when(emailUtil.send(TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        registerService.sendEmailCode(TEST_EMAIL, request);

        verify(valueOperations).set("email:register:" + TEST_IP, "1", 24, TimeUnit.HOURS);
        verify(valueOperations).set("email:register:" + TEST_DAY, "1", 24, TimeUnit.HOURS);
        verify(valueOperations).set("email:register:" + TEST_EMAIL, TEST_VERIFY_CODE, 5, TimeUnit.MINUTES);
    }

    @Test
    public void testSendEmailCode_IpCountUnderLimit_DayCountBlank_Success() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("4");
        when(valueOperations.get("email:register:" + TEST_DAY)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(null);
        when(emailUtil.send(TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        registerService.sendEmailCode(TEST_EMAIL, request);

        verify(valueOperations).set("email:register:" + TEST_IP, "5", 24, TimeUnit.HOURS);
        verify(valueOperations).set("email:register:" + TEST_DAY, "1", 24, TimeUnit.HOURS);
    }

    @Test
    public void testSendEmailCode_IpCountAtLimit_ThrowsException() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("5");

        try {
            registerService.sendEmailCode(TEST_EMAIL, request);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.REGISTER_IP_EMAIL_COUNT_00004, e.getReturnCode());
        }
    }

    @Test
    public void testSendEmailCode_IpCountOverLimit_ThrowsException() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("6");

        try {
            registerService.sendEmailCode(TEST_EMAIL, request);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.REGISTER_IP_EMAIL_COUNT_00004, e.getReturnCode());
        }
    }

    @Test
    public void testSendEmailCode_DayCountAtLimit_ThrowsException() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("1");
        when(valueOperations.get("email:register:" + TEST_DAY)).thenReturn("10");

        try {
            registerService.sendEmailCode(TEST_EMAIL, request);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.REGISTER_DAY_EMAIL_COUNT_00003, e.getReturnCode());
        }
    }

    @Test
    public void testSendEmailCode_DayCountOverLimit_ThrowsException() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("1");
        when(valueOperations.get("email:register:" + TEST_DAY)).thenReturn("11");

        try {
            registerService.sendEmailCode(TEST_EMAIL, request);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.REGISTER_DAY_EMAIL_COUNT_00003, e.getReturnCode());
        }
    }

    @Test
    public void testSendEmailCode_DayCountUnderLimit_Success() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("1");
        when(valueOperations.get("email:register:" + TEST_DAY)).thenReturn("9");
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(null);
        when(emailUtil.send(TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        registerService.sendEmailCode(TEST_EMAIL, request);

        verify(valueOperations).set("email:register:" + TEST_DAY, "10", 24, TimeUnit.HOURS);
    }

    @Test
    public void testSendEmailCode_EmailAlreadyUsed_ThrowsException() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("1");
        when(valueOperations.get("email:register:" + TEST_DAY)).thenReturn(null);
        UserInfo existingUserInfo = new UserInfo();
        existingUserInfo.setEmail(TEST_EMAIL);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(existingUserInfo);

        try {
            registerService.sendEmailCode(TEST_EMAIL, request);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_INFO_EXIST_EMAIL_00006, e.getReturnCode());
        }
    }

    @Test
    public void testSendEmailCode_AlreadySentRecently_ThrowsException() {
        when(valueOperations.get("email:register:" + TEST_IP)).thenReturn("1");
        when(valueOperations.get("email:register:" + TEST_DAY)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn("existingCode");

        try {
            registerService.sendEmailCode(TEST_EMAIL, request);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_EMAIL_SEND_CODE_FAIL_00028, e.getReturnCode());
        }
    }

    // ==================== verifyUsername tests ====================

    @Test
    public void testVerifyUsername_Blank_ThrowsException() {
        try {
            registerService.verifyUsername("");
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_NAME_IS_NULL_00008, e.getReturnCode());
        }
    }

    @Test
    public void testVerifyUsername_Null_ThrowsException() {
        try {
            registerService.verifyUsername(null);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_NAME_IS_NULL_00008, e.getReturnCode());
        }
    }

    @Test
    public void testVerifyUsername_AlreadyExists_ThrowsException() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(new User());

        try {
            registerService.verifyUsername(TEST_USERNAME);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_NAME_IS_NOT_NULL_00009, e.getReturnCode());
        }
    }

    @Test
    public void testVerifyUsername_Available_Success() {
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);

        registerService.verifyUsername(TEST_USERNAME);
    }

    // ==================== register tests ====================

    @Test
    public void testRegister_UsernameBlank_ThrowsException() {
        try {
            registerService.register("", TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_NULL_00011, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_PasswordBlank_ThrowsException() {
        try {
            registerService.register(TEST_USERNAME, "", TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_NULL_00011, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_NicknameBlank_ThrowsException() {
        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, "", TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_NULL_00011, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_EmailBlank_ThrowsException() {
        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, "", TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_NULL_00011, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_VerifyCodeBlank_ThrowsException() {
        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, "", session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_NULL_00011, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_AllParamsNull_ThrowsException() {
        try {
            registerService.register(null, null, null, null, null, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_NULL_00011, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_UsernameTooShort_ThrowsException() {
        try {
            registerService.register("ab", TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_LENGTH_00022, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_UsernameTooLong_ThrowsException() {
        try {
            registerService.register("123456789", TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_LENGTH_00022, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_UsernameNotAlphanumeric_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric("bad_user")).thenReturn(false);

        try {
            registerService.register("bad_user", TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_ALPHANUMERIC_00023, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_UsernameAlreadyExists_ThrowsException() {
        String existingUser = "existing";
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(existingUser)).thenReturn(true);
        when(userService.getUserByUsername(existingUser)).thenReturn(new User());

        try {
            registerService.register(existingUser, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_NAME_IS_NOT_NULL_00009, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_PasswordTooShort_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);

        try {
            registerService.register(TEST_USERNAME, "12345", TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_PasswordTooLong_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);

        try {
            registerService.register(TEST_USERNAME, "1234567890123456789", TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_NicknameTooLong_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);

        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, "1234567890123", TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_EmailAlreadyUsed_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        UserInfo existing = new UserInfo();
        existing.setEmail(TEST_EMAIL);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(existing);

        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_INFO_EXIST_EMAIL_00006, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_WrongVerifyCode_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn("654321");

        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_VerifyCodeNotInRedis_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(null);

        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_VerifyCodeBlankInRedis_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn("");

        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_DayUserCountExceeded_ThrowsException() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        List<User> dayUsers = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            dayUsers.add(new User());
        }
        when(userService.getUserListByDay(TEST_DAY)).thenReturn(dayUsers);
        when(systemConfig.getRegisterDayUserCount()).thenReturn(5);

        try {
            registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(UserReturnCode.USER_REGISTER_DAY_COUNT_MAX_00013, e.getReturnCode());
        }
    }

    @Test
    public void testRegister_DayUserCountEqualsLimit_Success() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);

        List<User> dayUsers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dayUsers.add(new User());
        }
        when(userService.getUserListByDay(TEST_DAY)).thenReturn(dayUsers);
        when(systemConfig.getRegisterDayUserCount()).thenReturn(5);

        Role role = new Role();
        role.setId(2L);
        when(roleService.list()).thenReturn(Collections.singletonList(role));
        when(roleService.getOne(any())).thenReturn(role);

        registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);

        verify(userService).insertUser(any(User.class));
        verify(userInfoService).insertUserInfo(any(UserInfo.class));
    }

    @Test
    public void testRegister_Success_RoleListNotEmpty() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);
        when(userService.getUserListByDay(TEST_DAY)).thenReturn(new ArrayList<>());
        when(systemConfig.getRegisterDayUserCount()).thenReturn(100);

        Role role = new Role();
        role.setId(2L);
        when(roleService.list()).thenReturn(Collections.singletonList(role));
        when(roleService.getOne(any())).thenReturn(role);

        registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);

        verify(userService).insertUser(any(User.class));
        verify(userInfoService).insertUserInfo(any(UserInfo.class));
        verify(userRoleService).save(any(UserRole.class));
        verify(session).setAttribute(eq("user"), any(User.class));
        verify(session).setMaxInactiveInterval(3600);
        verify(response).addCookie(any(Cookie.class));
        verify(valueOperations).set(eq("userLoginCipher" + TEST_USERNAME), anyString(), eq(120L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void testRegister_Success_RoleListEmpty() {
        stringUtilMock.when(() -> StringUtil.isAlphaNumeric(TEST_USERNAME)).thenReturn(true);
        when(userService.getUserByUsername(TEST_USERNAME)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail(TEST_EMAIL)).thenReturn(null);
        when(valueOperations.get("email:register:" + TEST_EMAIL)).thenReturn(TEST_VERIFY_CODE);
        when(userService.getUserListByDay(TEST_DAY)).thenReturn(new ArrayList<>());
        when(systemConfig.getRegisterDayUserCount()).thenReturn(100);

        Role adminRole = new Role();
        adminRole.setId(1L);
        when(roleService.list()).thenReturn(new ArrayList<>());
        when(roleService.getOne(any())).thenReturn(adminRole);

        registerService.register(TEST_USERNAME, TEST_PASSWORD, TEST_NICKNAME, TEST_EMAIL, TEST_VERIFY_CODE, session, response);

        verify(userService).insertUser(any(User.class));
        verify(userInfoService).insertUserInfo(any(UserInfo.class));
        verify(userRoleService).save(any(UserRole.class));
        verify(session).setAttribute(eq("user"), any(User.class));
        verify(session).setMaxInactiveInterval(3600);
        verify(response).addCookie(any(Cookie.class));
        verify(valueOperations).set(eq("userLoginCipher" + TEST_USERNAME), anyString(), eq(120L), eq(TimeUnit.MINUTES));
    }
}