package kr.co.seaduckene.user.mapper;

import java.util.List;
import java.util.Map;

import kr.co.seaduckene.common.CategoryVO;
import kr.co.seaduckene.user.command.Categories;
import kr.co.seaduckene.user.command.UserVO;

public interface IUserMapper {
	
	// 유저 생성
	void registUser(UserVO userVO);
	
	// 유저 정보 가져오기
	UserVO getUserVo(String userId);

	// 카테고리 정보 가져오기
	List<Categories> getCategories();
	
	// 카테고리 table에서 catogory_no 가져옴
	int getCategoryNo(CategoryVO boardCategoryVO);
	 
	// favorite table에 추가
	void insertFavorite(Map<String, Integer> map);
	 
}
