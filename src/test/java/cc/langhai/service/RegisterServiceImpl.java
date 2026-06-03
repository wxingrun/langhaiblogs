package cc.langhai.service;

import cc.langhai.config.system.SystemConfig;
import cc.langhai.domain.Role;
import cc.langhai.domain.User;
import cc.langhai.domain.UserInfo;
import cc.langhai.exception.BusinessException;
import cc.langhai.response.UserReturnCode;
import cc.langhai.utils.EmailUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegisterServiceImpl {

    @InjectMocks
    private cc.langhai.service.impl.RegisterServiceImpl registerService;

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
    public void sendEmailCodeShouldThrowWhenEmailBlank() {
        assertBusinessException(UserReturnCode.REGISTER_EMAIL_NULL_00005, () -> registerService.sendEmailCode("", request));
    }

    @Test
    public void sendEmailCodeShouldThrowWhenIpLimitReached() {
        Map<String, String> redis = new HashMap<>();
        redis.put("email:register:127.0.0.1", "3");
        mockRedis(redis);

        assertBusinessException(UserReturnCode.REGISTER_IP_EMAIL_COUNT_00004,
                () -> registerService.sendEmailCode("user@example.com", request));
    }

    @Test
    public void sendEmailCodeShouldThrowWhenDayLimitReached() {
        Map<String, String> redis = new HashMap<>();
        redis.put("email:register:127.0.0.1", "1");
        mockRedis(redis);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("email:register:127.0.0.1".equals(key)) {
                return "1";
            }
            if (key.startsWith("email:register:") && !key.endsWith("@example.com")) {
                return "5";
            }
            return redis.get(key);
        });

        assertBusinessException(UserReturnCode.REGISTER_DAY_EMAIL_COUNT_00003,
                () -> registerService.sendEmailCode("user@example.com", request));
    }

    @Test
    public void sendEmailCodeShouldThrowWhenEmailAlreadyUsed() {
        Map<String, String> redis = new HashMap<>();
        mockRedis(redis);
        when(userInfoService.getUserInfoByEmail("user@example.com")).thenReturn(new UserInfo());

        assertBusinessException(UserReturnCode.USER_INFO_EXIST_EMAIL_00006,
                () -> registerService.sendEmailCode("user@example.com", request));
    }

    @Test
    public void sendEmailCodeShouldThrowWhenCodeAlreadySent() {
        Map<String, String> redis = new HashMap<>();
        redis.put("email:register:user@example.com", "123456");
        mockRedis(redis);

        assertBusinessException(UserReturnCode.USER_EMAIL_SEND_CODE_FAIL_00028,
                () -> registerService.sendEmailCode("user@example.com", request));
    }

    @Test
    public void sendEmailCodeShouldPropagateBusinessExceptionForInvalidEmail() {
        Map<String, String> redis = new HashMap<>();
        mockRedis(redis);
        when(emailUtil.send("bad-email")).thenThrow(new BusinessException(UserReturnCode.EMAIL_CODE_00001));

        assertBusinessException(UserReturnCode.EMAIL_CODE_00001,
                () -> registerService.sendEmailCode("bad-email", request));
    }

    @Test
    public void sendEmailCodeShouldSaveCodeWhenFirstSend() {
        Map<String, String> redis = new HashMap<>();
        mockRedis(redis);
        when(emailUtil.send("user@example.com")).thenReturn("654321");

        registerService.sendEmailCode("user@example.com", request);

        assertEquals("1", redis.get("email:register:127.0.0.1"));
        assertEquals("654321", redis.get("email:register:user@example.com"));
    }

    @Test
    public void sendEmailCodeShouldIncrementIpAndDayCountersWhenHistoryExists() {
        Map<String, String> redis = new HashMap<>();
        redis.put("email:register:127.0.0.1", "1");
        redis.put("email:register:day-key", "2");
        mockRedis(redis);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("email:register:127.0.0.1".equals(key)) {
                return "1";
            }
            if ("email:register:user@example.com".equals(key)) {
                return redis.get(key);
            }
            if (key.startsWith("email:register:")) {
                return "2";
            }
            return redis.get(key);
        });
        when(emailUtil.send("user@example.com")).thenReturn("222333");

        registerService.sendEmailCode("user@example.com", request);

        assertEquals("2", redis.get("email:register:127.0.0.1"));
        assertEquals("222333", redis.get("email:register:user@example.com"));
    }

    @Test
    public void verifyUsernameShouldThrowWhenBlank() {
        assertBusinessException(UserReturnCode.USER_NAME_IS_NULL_00008, () -> registerService.verifyUsername(""));
    }

    @Test
    public void verifyUsernameShouldThrowWhenDuplicated() {
        when(userService.getUserByUsername("langhai")).thenReturn(new User());

        assertBusinessException(UserReturnCode.USER_NAME_IS_NOT_NULL_00009, () -> registerService.verifyUsername("langhai"));
    }

    @Test
    public void verifyUsernameShouldPassWhenAvailable() {
        registerService.verifyUsername("langhai");
    }

    @Test
    public void registerShouldThrowWhenParamBlank() {
        assertBusinessException(UserReturnCode.USER_REGISTER_PARAM_NULL_00011,
                () -> registerService.register("", "123456", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenUsernameLengthInvalid() {
        assertBusinessException(UserReturnCode.USER_REGISTER_PARAM_VERIFY_LENGTH_00022,
                () -> registerService.register("ab", "123456", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenUsernameNotAlphaNumeric() {
        assertBusinessException(UserReturnCode.USER_REGISTER_PARAM_VERIFY_ALPHANUMERIC_00023,
                () -> registerService.register("ab-1", "123456", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenUsernameAlreadyExists() {
        when(userService.getUserByUsername("user123")).thenReturn(new User());

        assertBusinessException(UserReturnCode.USER_NAME_IS_NOT_NULL_00009,
                () -> registerService.register("user123", "123456", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenPasswordLengthInvalid() {
        assertBusinessException(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012,
                () -> registerService.register("user123", "12345", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenNicknameLengthInvalid() {
        assertBusinessException(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012,
                () -> registerService.register("user123", "123456", "nickname-over", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenEmailAlreadyExists() {
        when(userInfoService.getUserInfoByEmail("user@example.com")).thenReturn(new UserInfo());

        assertBusinessException(UserReturnCode.USER_INFO_EXIST_EMAIL_00006,
                () -> registerService.register("user123", "123456", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenVerifyCodeMissing() {
        Map<String, String> redis = new HashMap<>();
        mockRedis(redis);

        assertBusinessException(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012,
                () -> registerService.register("user123", "123456", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenVerifyCodeWrong() {
        Map<String, String> redis = new HashMap<>();
        redis.put("email:register:user@example.com", "654321");
        mockRedis(redis);

        assertBusinessException(UserReturnCode.USER_REGISTER_PARAM_VERIFY_00012,
                () -> registerService.register("user123", "123456", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldThrowWhenDayUserCountExceeded() {
        Map<String, String> redis = new HashMap<>();
        redis.put("email:register:user@example.com", "123456");
        mockRedis(redis);
        when(userService.getUserListByDay(anyString())).thenReturn(Arrays.asList(new User(), new User(), new User()));

        assertBusinessException(UserReturnCode.USER_REGISTER_DAY_COUNT_MAX_00013,
                () -> registerService.register("user123", "123456", "nick", "user@example.com", "123456", session, response));
    }

    @Test
    public void registerShouldSaveUserAndAssignAdminRoleWhenRoleListEmpty() {
        Map<String, String> redis = new HashMap<>();
        redis.put("email:register:user@example.com", "123456");
        mockRedis(redis);
        when(userService.getUserListByDay(anyString())).thenReturn(Collections.singletonList(new User()));
        when(roleService.list()).thenReturn(Collections.emptyList());
        when(roleService.getOne(any())).thenReturn(new Role());

        registerService.register("user123", "123456", "nick", "user@example.com", "123456", session, response);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).insertUser(userCaptor.capture());
        assertEquals("user123", userCaptor.getValue().getUsername());
        assertNotEquals("123456", userCaptor.getValue().getPassword());
        verify(userInfoService).insertUserInfo(any(UserInfo.class));
        verify(userRoleService).save(any());
        verify(session).setAttribute(anyString(), any(User.class));
        verify(session).setMaxInactiveInterval(anyInt());
        verify(response).addCookie(any());
    }

    @Test
    public void registerShouldAssignUserRoleWhenRoleListExists() {
        Map<String, String> redis = new HashMap<>();
        redis.put("email:register:user2@example.com", "123456");
        mockRedis(redis);
        when(userService.getUserListByDay(anyString())).thenReturn(Collections.singletonList(new User()));
        when(roleService.list()).thenReturn(Collections.singletonList(new Role()));
        when(roleService.getOne(any())).thenReturn(new Role());

        registerService.register("user456", "123456", "nick", "user2@example.com", "123456", session, response);

        verify(userRoleService).save(any());
    }

    private void mockRedis(Map<String, String> redis) {
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redis.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            redis.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    private void assertBusinessException(Object expectedCode, Runnable runnable) {
        try {
            runnable.run();
            fail();
        } catch (BusinessException exception) {
            assertSame(expectedCode, exception.getReturnCode());
        }
    }
}
