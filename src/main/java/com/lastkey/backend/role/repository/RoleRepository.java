package com.lastkey.backend.role.repository;

import com.lastkey.backend.common.enums.RoleType;
import com.lastkey.backend.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);

}