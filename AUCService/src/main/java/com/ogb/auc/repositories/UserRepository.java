package com.ogb.auc.repositories;


import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.ogb.auc.domains.User;


@Transactional
public interface UserRepository extends CrudRepository<User, String>
{
	public User findByToken(String token);
	public User findByUserID(String userID);
	public User findByPrivateKey(byte[] privateKey);
}
