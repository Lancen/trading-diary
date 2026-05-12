package com.tradingdiary.security;

import com.tradingdiary.entity.SysRole;
import com.tradingdiary.entity.SysUser;
import com.tradingdiary.mapper.SysRoleMapper;
import com.tradingdiary.mapper.SysUserMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    public UserDetailsServiceImpl(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.selectByUsername(username);
        return buildUserDetails(sysUser, username);
    }

    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.selectById(userId);
        return buildUserDetails(sysUser, String.valueOf(userId));
    }

    private UserDetails buildUserDetails(SysUser sysUser, String identifier) {
        if (sysUser == null || Boolean.TRUE.equals(sysUser.getIsDeleted())) {
            throw new UsernameNotFoundException("User not found: " + identifier);
        }

        if (sysUser.getStatus() == 0) {
            throw new DisabledException("User is disabled: " + identifier);
        }

        List<SysRole> roles = sysRoleMapper.selectByUserId(sysUser.getId());
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toList());

        return new User(sysUser.getUsername(), sysUser.getPassword(), authorities);
    }
}
