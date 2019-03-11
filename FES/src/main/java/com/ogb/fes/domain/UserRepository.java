package com.ogb.fes.domain;


import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;


@Transactional
public interface UserRepository extends CrudRepository<User, String>
{
	public User findByToken(String token);
	public User findByUserID(String userID);
}
