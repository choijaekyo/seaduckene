package kr.co.seaduckene.user.mapper;

import java.util.List;

import kr.co.seaduckene.user.command.Categories;
import kr.co.seaduckene.user.command.UserVO;

public interface IUserMapper {
	
	// 유저 생성
	void registUser(UserVO userVO);
	
	// 카테고리 정보 가져오기
	List<Categories> getCategories();
}
