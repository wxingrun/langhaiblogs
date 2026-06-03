package cc.langhai.service;

import cc.langhai.config.system.SystemConfig;
import cc.langhai.domain.Role;
import cc.langhai.domain.User;
import cc.langhai.domain.UserInfo;
import cc.langhai.exception.BusinessException;
import cc.langhai.response.UserReturnCode;
import cc.langhai.service.RoleService;
import cc.langhai.service.UserInfoService;
import cc.langhai.service.UserRoleService;
import cc.langhai.service.UserService;
import cc.langhai.utils.EmailUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegisterServiceImpl {

    @Spy
    @InjectMocks
    private cc.langhai.service.impl.RegisterServiceImpl registerService = new cc.langhai.service.impl.RegisterServiceImpl();

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

    @Before
    public void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(systemConfig.getSecret()).thenReturn("1234567890123456");
        when(systemConfig.getRegisterIPEmailCount()).thenReturn(3);
        when(systemConfig.getRegisterDayEmailCount()).thenReturn(5);
        when(systemConfig.getRegisterDayUserCount()).thenReturn(2);
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    public void sendEmailCodeThrowsWhenEmailIsBlank() {
        BusinessException exception = expectBusinessException(() -> registerService.sendEmailCode("", request));

        Assert.assertSame(UserReturnCode.REGISTER_EMAIL_NULL_00005, exception.getReturnCode());
    }

    @Test
    public void sendEmailCodeThrowsWhenIpLimitReached() {
        stubSendEmailRedis("test@example.com", "3", null, null);

        BusinessException exception = expectBusinessException(() -> registerService.sendEmailCode("test@example.com", request));

        Assert.assertSame(UserReturnCode.REGISTER_IP_EMAIL_COUNT_00004, exception.getReturnCode());
    }

    @Test
    public void sendEmailCodeThrowsWhenDayLimitReached() {
        stubSendEmailRedis("test@example.com", null, "5", null);

        BusinessException exception = expectBusinessException(() -> registerService.sendEmailCode("test@example.com", request));

        Assert.assertSame(UserReturnCode.REGISTER_DAY_EMAIL_COUNT_00003, exception.getReturnCode());
    }

    @Test
    public void sendEmailCodeThrowsWhenEmailAlreadyExists() {
        stubSendEmailRedis("test@example.com", null, null, null);
        when(userInfoService.getUserInfoByEmail("test@example.com")).thenReturn(new UserInfo());

        BusinessException exception = expectBusinessException(() -> registerService.sendEmailCode("test@example.com", request));

        Assert.assertSame(UserReturnCode.USER_INFO_EXIST_EMAIL_00006, exception.getReturnCode());
    }

    @Test
    public void sendEmailCodeThrowsWhenCodeSentRepeatedly() {
        stubSendEmailRedis("test@example.com", null, null, "654321");

        BusinessException exception = expectBusinessException(() -> registerService.sendEmailCode("test@example.com", request));

        Assert.assertSame(UserReturnCode.USER_EMAIL_SEND_CODE_FAIL_00028, exception.getReturnCode());
    }

    @Test
    public void sendEmailCodePropagatesMailFailure() {
        stubSendEmailRedis("bad-email", null, null, null);
        BusinessException mailException = new BusinessException(UserReturnCode.EMAIL_CODE_00001);
        when(emailUtil.send("bad-email")).thenThrow(mailException);

        BusinessException exception = expectBusinessException(() -> registerService.sendEmailCode("bad-email", request));

        Assert.assertSame(UserReturnCode.EMAIL_CODE_00001, exception.getReturnCode());
    }

    @Test
    public void sendEmailCodeStoresCodeForFirstSend() {
        stubSendEmailRedis("first@example.com", null, null, null);
        when(emailUtil.send("first@example.com")).thenReturn("123456");

        registerService.sendEmailCode("first@example.com", request);

        verify(valueOperations).set(eq("email:register:127.0.0.1"), eq("1"), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(eq("email:register:" + cc.langhai.utils.DateUtil.getNowDay()), eq("1"), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(eq("email:register:first@example.com"), eq("123456"), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void sendEmailCodeIncrementsExistingCountersBeforeSending() {
        stubSendEmailRedis("next@example.com", "1", "2", null);
        when(emailUtil.send("next@example.com")).thenReturn("222222");

        registerService.sendEmailCode("next@example.com", request);

        verify(valueOperations).set(eq("email:register:127.0.0.1"), eq("2"), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(eq("email:register:" + cc.langhai.utils.DateUtil.getNowDay()), eq("3"), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(eq("email:register:next@example.com"), eq("222222"), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void verifyUsernameThrowsWhenBlank() {
        BusinessException exception = expectBusinessException(() -> registerService.verifyUsername(" "));

        Assert.assertSame(UserReturnCode.USER_NAME_IS_NULL_00008, exception.getReturnCode());
    }

    @Test
    public void verifyUsernameThrowsWhenDuplicated() {
        when(userService.getUserByUsername("langhai")).thenReturn(new User());

        BusinessException exception = expectBusinessException(() -> registerService.verifyUsername("langhai"));

        Assert.assertSame(UserReturnCode.USER_NAME_IS_NOT_NULL_00009, exception.getReturnCode());
    }

    @Test
    public void verifyUsernamePassesWhenAvailable() {
        registerService.verifyUsername("langhai");

        verify(userService).getUserByUsername("langhai");
    }

    @Test
    public void registerThrowsWhenAnyParamIsBlank() {
        BusinessException exception = expectBusinessException(() -> registerService.register("", "123456", "nick", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_REGISTER_PARAM_NULL_00011, exception.getReturnCode());
    }

    @Test
    public void registerThrowsWhenUsernameLengthInvalid() {
        BusinessException exception = expectBusinessException(() -> registerService.register("ab", "123456", "nick", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_REGISTER_PARAM_VERIFY_LENGTH_00022, exception.getReturnCode());
    }

    @Test
    public void registerThrowsWhenUsernameIsNotAlphaNumeric() {
        BusinessException exception = expectBusinessException(() -> registerService.register("ab-1", "123456", "nick", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_REGISTER_PARAM_VERIFY_ALPHANUMERIC_00023, exception.getReturnCode());
    }

    @Test
    public void registerThrowsWhenUsernameAlreadyExists() {
        when(userService.getUserByUsername("abc123")).thenReturn(new User());

        BusinessException exception = expectBusinessException(() -> registerService.register("abc123", "123456", "nick", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_NAME_IS_NOT_NULL_00009, exception.getReturnCode());
    }

    @Test
    public void registerThrowsWhenPasswordLengthInvalid() {
        BusinessException exception = expectBusinessException(() -> registerService.register("abc123", "12345", "nick", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, exception.getReturnCode());
    }

    @Test
    public void registerThrowsWhenNicknameLengthInvalid() {
        BusinessException exception = expectBusinessException(() -> registerService.register("abc123", "123456", "nickname-too-long", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, exception.getReturnCode());
    }

    @Test
    public void registerThrowsWhenEmailAlreadyExists() {
        when(userInfoService.getUserInfoByEmail("mail@example.com")).thenReturn(new UserInfo());

        BusinessException exception = expectBusinessException(() -> registerService.register("abc123", "123456", "nick", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_INFO_EXIST_EMAIL_00006, exception.getReturnCode());
    }

    @Test
    public void registerThrowsWhenVerifyCodeIsWrong() {
        when(valueOperations.get("email:register:mail@example.com")).thenReturn("654321");

        BusinessException exception = expectBusinessException(() -> registerService.register("abc123", "123456", "nick", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012, exception.getReturnCode());
    }

    @Test
    public void registerThrowsWhenDayUserCountExceeded() {
        when(valueOperations.get("email:register:mail@example.com")).thenReturn("123456");
        when(userService.getUserListByDay(anyString())).thenReturn(Arrays.asList(new User(), new User(), new User()));

        BusinessException exception = expectBusinessException(() -> registerService.register("abc123", "123456", "nick", "mail@example.com", "123456", session, response));

        Assert.assertSame(UserReturnCode.USER_REGISTER_DAY_COUNT_MAX_00013, exception.getReturnCode());
    }

    @Test
    public void registerAssignsAdminRoleWhenRoleListIsEmpty() {
        stubRegisterSuccessBase();
        when(roleService.list()).thenReturn(Collections.emptyList());
        Role role = org.mockito.Mockito.mock(Role.class);
        when(roleService.getOne(any())).thenReturn(role);
        doNothing().when(registerService).temporaryRemember("abc123", response);

        registerService.register("abc123", "123456", "nick", "mail@example.com", "123456", session, response);

        assertSuccessfulRegister("abc123", "123456", "nick", "mail@example.com");
        verify(roleService).list();
        verify(roleService).getOne(any());
        verify(userRoleService).save(any(cc.langhai.domain.UserRole.class));
        verify(registerService).temporaryRemember("abc123", response);
    }

    @Test
    public void registerAssignsUserRoleWhenRoleListExists() {
        stubRegisterSuccessBase();
        when(roleService.list()).thenReturn(Collections.singletonList(org.mockito.Mockito.mock(Role.class)));
        Role role = org.mockito.Mockito.mock(Role.class);
        when(roleService.getOne(any())).thenReturn(role);
        doNothing().when(registerService).temporaryRemember("abc123", response);

        registerService.register("abc123", "123456", "nick", "mail@example.com", "123456", session, response);

        assertSuccessfulRegister("abc123", "123456", "nick", "mail@example.com");
        verify(roleService).list();
        verify(roleService).getOne(any());
        verify(userRoleService).save(any(cc.langhai.domain.UserRole.class));
        verify(registerService, times(1)).temporaryRemember("abc123", response);
    }

    private void stubSendEmailRedis(final String email, final String ipCount, final String dayCount, final String emailSendCode) {
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (("email:register:127.0.0.1").equals(key)) {
                return ipCount;
            }
            if (("email:register:" + cc.langhai.utils.DateUtil.getNowDay()).equals(key)) {
                return dayCount;
            }
            if (("email:register:" + email).equals(key)) {
                return emailSendCode;
            }
            return null;
        });
    }

    private void stubRegisterSuccessBase() {
        when(valueOperations.get("email:register:mail@example.com")).thenReturn("123456");
        when(userService.getUserListByDay(anyString())).thenReturn(Collections.<User>emptyList());
    }

    private void assertSuccessfulRegister(String username, String password, String nickname, String email) {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).insertUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        Assert.assertEquals(username, savedUser.getUsername());
        Assert.assertEquals(nickname, savedUser.getNickname());
        Assert.assertTrue(savedUser.getEnable());
        Assert.assertFalse(savedUser.getImage());
        Assert.assertEquals(new SymmetricCrypto(SymmetricAlgorithm.AES, "1234567890123456".getBytes()).encryptHex(password), savedUser.getPassword());
        Assert.assertNotNull(savedUser.getAddTime());
        Assert.assertNotNull(savedUser.getAddTimeShow());

        ArgumentCaptor<UserInfo> userInfoCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoService).insertUserInfo(userInfoCaptor.capture());
        Assert.assertEquals(email, userInfoCaptor.getValue().getEmail());

        verify(session).setAttribute(eq("user"), any(User.class));
        verify(session).setMaxInactiveInterval(60 * 60);
    }

    private BusinessException expectBusinessException(Runnable runnable) {
        try {
            runnable.run();
        } catch (BusinessException exception) {
            return exception;
        }
        Assert.fail("Expected BusinessException");
        return null;
    }
}
