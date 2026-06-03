package cc.langhai.service;

import cc.langhai.config.constant.UserConstant;
import cc.langhai.config.system.SystemConfig;
import cc.langhai.domain.Role;
import cc.langhai.domain.User;
import cc.langhai.domain.UserInfo;
import cc.langhai.domain.UserRole;
import cc.langhai.exception.BusinessException;
import cc.langhai.utils.EmailUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_EmailBlank() {
        registerService.sendEmailCode("", request);
    }

    @Test
    public void testSendEmailCode_Success_NewIp_NewDay() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn(null);
        when(valueOperations.get("email:register:127.0.0.1")).thenReturn(null);
        
        String nowDay = cc.langhai.utils.DateUtil.getNowDay();
        when(valueOperations.get("email:register:" + nowDay)).thenReturn(null);
        when(userInfoService.getUserInfoByEmail("test@test.com")).thenReturn(null);
        when(valueOperations.get("email:register:test@test.com")).thenReturn(null);
        when(emailUtil.send("test@test.com")).thenReturn("123456");
        
        registerService.sendEmailCode("test@test.com", request);
        
        verify(valueOperations).set("email:register:127.0.0.1", "1", 24, TimeUnit.HOURS);
        verify(valueOperations).set("email:register:" + nowDay, "1", 24, TimeUnit.HOURS);
        verify(valueOperations).set("email:register:test@test.com", "123456", 5, TimeUnit.MINUTES);
    }

    @Test
    public void testSendEmailCode_Success_OldIp_OldDay() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("127.0.0.2");
        when(valueOperations.get("email:register:127.0.0.2")).thenReturn("1");
        when(systemConfig.getRegisterIPEmailCount()).thenReturn(5);
        
        String nowDay = cc.langhai.utils.DateUtil.getNowDay();
        when(valueOperations.get("email:register:" + nowDay)).thenReturn("1");
        when(systemConfig.getRegisterDayEmailCount()).thenReturn(5);
        
        when(userInfoService.getUserInfoByEmail("test@test.com")).thenReturn(null);
        when(valueOperations.get("email:register:test@test.com")).thenReturn(null);
        when(emailUtil.send("test@test.com")).thenReturn("123456");
        
        registerService.sendEmailCode("test@test.com", request);
        
        verify(valueOperations).set("email:register:127.0.0.2", "2", 24, TimeUnit.HOURS);
        verify(valueOperations).set("email:register:" + nowDay, "2", 24, TimeUnit.HOURS);
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_IpCountExceed() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn(null);
        when(valueOperations.get("email:register:127.0.0.1")).thenReturn("5");
        when(systemConfig.getRegisterIPEmailCount()).thenReturn(5);
        registerService.sendEmailCode("test@test.com", request);
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_DayCountExceed() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn(null);
        when(valueOperations.get("email:register:127.0.0.1")).thenReturn("1");
        when(systemConfig.getRegisterIPEmailCount()).thenReturn(5);
        
        String nowDay = cc.langhai.utils.DateUtil.getNowDay();
        when(valueOperations.get("email:register:" + nowDay)).thenReturn("5");
        when(systemConfig.getRegisterDayEmailCount()).thenReturn(5);
        
        registerService.sendEmailCode("test@test.com", request);
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_UserInfoExist() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn(null);
        when(valueOperations.get("email:register:127.0.0.1")).thenReturn("1");
        when(systemConfig.getRegisterIPEmailCount()).thenReturn(5);
        
        String nowDay = cc.langhai.utils.DateUtil.getNowDay();
        when(valueOperations.get("email:register:" + nowDay)).thenReturn("1");
        when(systemConfig.getRegisterDayEmailCount()).thenReturn(5);
        
        when(userInfoService.getUserInfoByEmail("test@test.com")).thenReturn(new UserInfo());
        
        registerService.sendEmailCode("test@test.com", request);
    }

    @Test(expected = BusinessException.class)
    public void testSendEmailCode_AlreadySent() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn(null);
        when(valueOperations.get("email:register:127.0.0.1")).thenReturn("1");
        when(systemConfig.getRegisterIPEmailCount()).thenReturn(5);
        
        String nowDay = cc.langhai.utils.DateUtil.getNowDay();
        when(valueOperations.get("email:register:" + nowDay)).thenReturn("1");
        when(systemConfig.getRegisterDayEmailCount()).thenReturn(5);
        
        when(userInfoService.getUserInfoByEmail("test@test.com")).thenReturn(null);
        when(valueOperations.get("email:register:test@test.com")).thenReturn("123456");
        
        registerService.sendEmailCode("test@test.com", request);
    }

    @Test(expected = BusinessException.class)
    public void testVerifyUsername_Null() {
        registerService.verifyUsername("");
    }

    @Test(expected = BusinessException.class)
    public void testVerifyUsername_Exists() {
        when(userService.getUserByUsername("testuser")).thenReturn(new User());
        registerService.verifyUsername("testuser");
    }

    @Test
    public void testVerifyUsername_Success() {
        when(userService.getUserByUsername("testuser")).thenReturn(null);
        registerService.verifyUsername("testuser");
        verify(userService).getUserByUsername("testuser");
    }

    @Test(expected = BusinessException.class)
    public void testRegister_ParamNull_Username() {
        registerService.register("", "password", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_ParamNull_Password() {
        registerService.register("user", "", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_ParamNull_Nickname() {
        registerService.register("user", "pass", "", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_ParamNull_Email() {
        registerService.register("user", "pass", "nick", "", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_ParamNull_VerifyCodeText() {
        registerService.register("user", "pass", "nick", "email", "", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_UsernameTooShort() {
        registerService.register("us", "password", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_UsernameTooLong() {
        registerService.register("useruser1", "password", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_UsernameNotAlphanumeric() {
        registerService.register("us#r", "password", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_PasswordTooShort() {
        registerService.register("user", "passw", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_PasswordTooLong() {
        registerService.register("user", "passwordpassword123", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_NicknameTooLong() {
        when(userService.getUserByUsername("user")).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("1234567890123456");
        registerService.register("user", "password", "nickname123456", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_EmailExists() {
        when(userService.getUserByUsername("user")).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("1234567890123456");
        when(userInfoService.getUserInfoByEmail("email")).thenReturn(new UserInfo());
        registerService.register("user", "password", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_VerifyCodeNull() {
        when(userService.getUserByUsername("user")).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("1234567890123456");
        when(userInfoService.getUserInfoByEmail("email")).thenReturn(null);
        when(valueOperations.get("email:register:email")).thenReturn(null);
        registerService.register("user", "password", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_VerifyCodeWrong() {
        when(userService.getUserByUsername("user")).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("1234567890123456");
        when(userInfoService.getUserInfoByEmail("email")).thenReturn(null);
        when(valueOperations.get("email:register:email")).thenReturn("wrong");
        registerService.register("user", "password", "nick", "email", "code", session, response);
    }

    @Test(expected = BusinessException.class)
    public void testRegister_DayCountExceed() {
        when(userService.getUserByUsername("user")).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("1234567890123456");
        when(userInfoService.getUserInfoByEmail("email")).thenReturn(null);
        when(valueOperations.get("email:register:email")).thenReturn("code");
        
        String nowDay = cc.langhai.utils.DateUtil.getNowDay();
        List<User> userList = new ArrayList<>();
        userList.add(new User());
        userList.add(new User());
        when(userService.getUserListByDay(nowDay)).thenReturn(userList);
        when(systemConfig.getRegisterDayUserCount()).thenReturn(1);
        
        registerService.register("user", "password", "nick", "email", "code", session, response);
    }

    @Test
    public void testRegister_Success_RoleEmpty() {
        when(userService.getUserByUsername("user")).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("1234567890123456");
        when(userInfoService.getUserInfoByEmail("email")).thenReturn(null);
        when(valueOperations.get("email:register:email")).thenReturn("code");
        
        String nowDay = cc.langhai.utils.DateUtil.getNowDay();
        when(userService.getUserListByDay(nowDay)).thenReturn(new ArrayList<>());
        when(systemConfig.getRegisterDayUserCount()).thenReturn(5);
        
        when(roleService.list()).thenReturn(new ArrayList<>());
        Role adminRole = new Role();
        adminRole.setId(1L);
        when(roleService.getOne(any())).thenReturn(adminRole);
        
        registerService.register("user", "password", "nick", "email", "code", session, response);
        
        verify(userService).insertUser(any(User.class));
        verify(userInfoService).insertUserInfo(any(UserInfo.class));
        verify(userRoleService).save(any(UserRole.class));
        verify(session).setAttribute(eq("user"), any(User.class));
        verify(session).setMaxInactiveInterval(60 * 60);
        verify(valueOperations).set(eq("userLoginCipheruser"), anyString(), eq(120L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void testRegister_Success_RoleNotEmpty() {
        when(userService.getUserByUsername("user")).thenReturn(null);
        when(systemConfig.getSecret()).thenReturn("1234567890123456");
        when(userInfoService.getUserInfoByEmail("email")).thenReturn(null);
        when(valueOperations.get("email:register:email")).thenReturn("code");
        
        String nowDay = cc.langhai.utils.DateUtil.getNowDay();
        when(userService.getUserListByDay(nowDay)).thenReturn(new ArrayList<>());
        when(systemConfig.getRegisterDayUserCount()).thenReturn(5);
        
        List<Role> roleList = new ArrayList<>();
        roleList.add(new Role());
        when(roleService.list()).thenReturn(roleList);
        Role userRole = new Role();
        userRole.setId(2L);
        when(roleService.getOne(any())).thenReturn(userRole);
        
        registerService.register("user", "password", "nick", "email", "code", session, response);
        
        verify(userService).insertUser(any(User.class));
        verify(userInfoService).insertUserInfo(any(UserInfo.class));
        verify(userRoleService).save(any(UserRole.class));
    }
}