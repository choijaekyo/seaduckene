package kr.co.seaduckene.user;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import kr.co.seaduckene.board.command.BoardVO;
import kr.co.seaduckene.board.service.IBoardService;
import kr.co.seaduckene.common.AddressVO;
import kr.co.seaduckene.common.CategoryVO;
import kr.co.seaduckene.product.command.ProductBasketVO;
import kr.co.seaduckene.user.command.UserVO;
import kr.co.seaduckene.user.service.IUserService;
import kr.co.seaduckene.util.CertificationMailService;
import kr.co.seaduckene.util.PageVO;
import lombok.extern.log4j.Log4j;

@Controller
@RequestMapping("/user")
@Log4j
public class UserController {
	
	@Autowired
	private IBoardService boardService;
	
	@Autowired
	private IUserService userService;
	
	@Autowired
	private CertificationMailService mailService;
	

	@GetMapping("/userLogin")
	public void userLogin() {}
	
	@PostMapping("/userLoginAuth")
	public ModelAndView userLogin(UserVO userVO, ModelAndView modelAndView, int autoLoginCheck) {
		log.info(userVO);
		
		// 비밀번호 암호화는 나중에 구현할 것.
		
		modelAndView.addObject("userVo", userService.getUserVo(userVO));
		modelAndView.addObject("autoLoginCheck", autoLoginCheck);
		
		return modelAndView;
	}
	
	@GetMapping("/userJoin")
	public void userJoin(HttpServletRequest request) {
		log.info(userService.getCategories());
		
		request.setAttribute("categoryList", userService.getCategories());
		request.setAttribute("majorLength", userService.getCategories().size() - 1);
		log.info(userService.getCategories().size() - 1);
	}
	
	@GetMapping("/userLogout")
	public ModelAndView userLogin(ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response) {
		
		HttpSession session = request.getSession();
		
		Cookie autoLoginCookie = WebUtils.getCookie(request, "autoLoginCookie");
		if (autoLoginCookie != null) {
			UserVO userVo = userService.getUserBySessionId(autoLoginCookie.getValue());
			log.info("autoLogin userVo: " + userVo);
			
			if (userVo != null) {	
				// 쿠키 삭제는 받아온 쿠키 객체를 직접 지운다
				autoLoginCookie.setPath(request.getContextPath() + "/");
				autoLoginCookie.setMaxAge(0);
				response.addCookie(autoLoginCookie);
				userService.undoAutoLogin(userVo.getUserNo());
			}
			
		}
		
		
		session.removeAttribute("login");
		modelAndView.setViewName("redirect:/user/userLogin");
		
		return modelAndView;
	}
	
	@PostMapping("/userJoin")
	public ModelAndView userjoin(UserVO userVO, AddressVO addressVO, CategoryVO  boardCategoryVO, ModelAndView modelAndView, MultipartFile profilePic) {
		log.info(userVO);
		log.info(addressVO);
		log.info(boardCategoryVO);
		log.info(profilePic);
		
		if (profilePic.getSize() != 0) {
			SimpleDateFormat simple = new SimpleDateFormat("yyyyMMdd");
			String today = simple.format(new Date());
			
			String fileRealName = profilePic.getOriginalFilename(); // 파일 원본명
			String profilePath = "c:/imgduck/user/";
			
			String fileExtension = fileRealName.substring(fileRealName.lastIndexOf("."),fileRealName.length());
			
			UUID uuid = UUID.randomUUID();
			String uu = uuid.toString().replace("-","");
			
			
			userVO.setUserProfileFileRealName(fileRealName);
			userVO.setUserProfilePath(profilePath);
			userVO.setUserProfileFolder(today);
			userVO.setUserProfileFileName(uu + fileExtension); 
			
			String uploadFolder = profilePath + today;
			File folder = new File(uploadFolder);
			if(!folder.exists()) {
				folder.mkdirs();
			}
			File saveFile = new File(uploadFolder+"/"+uu+fileExtension);
			try {
				profilePic.transferTo(saveFile);
			} catch (IllegalStateException | IOException e) {
				e.printStackTrace();
			}
		}
		
		//user table 등록
		userService.registUser(userVO);
		
		// 다른 곳에서 user정보가 필요할 시, 로그인 중인 세션에서 uservo 갖고 올 예정.
		// - 안됨. userno에서 uservo를 가져와야 최신 유저 정보를 쓸 수 있다.
		// 계정 생성중에는 세션 정보가 없다.
		UserVO registeredUserVO = userService.getUserVo(userVO);
		int registerdUserNo = registeredUserVO.getUserNo();
		addressVO.setAddressUserNo(registerdUserNo);
		
		//favorite table 등록
		userService.updateUserFavorites(boardCategoryVO, registerdUserNo);
		
		if (!addressVO.getAddressBasic().equals("")) {
			// address table 등록
			userService.registAddr(addressVO);
		}
		
		modelAndView.setViewName("redirect:/user/userJoinSuccess");
		
		return modelAndView;
	}
	
	@GetMapping("/userJoinSuccess")
	public void userJoinSuccess() {};
	
	// email인증
	@ResponseBody
	@PostMapping("/userConfEmail")
	public String userConfEmail(@RequestBody String email) {
		log.info("email인증요청 들어옴" + email);
		return mailService.joinEmail(email);
	}

	@GetMapping("/userMyPage/{head}")
	public ModelAndView userMyPage(ModelAndView modelAndView, @PathVariable int head, HttpSession session) {
		modelAndView.addObject("toggle", head);
		
		modelAndView.setViewName("/user/userMyPage");
		UserVO vo = (UserVO)session.getAttribute("login");
		int userNo = vo.getUserNo();
		List<ProductBasketVO> bvo = userService.getBasket(userNo);
		int total = 0;
		for(ProductBasketVO b : bvo) {
			total += b.getBasketQuantity() * b.getBasketPrice();
		}
		modelAndView.addObject("basket", bvo);
		modelAndView.addObject("total", total);
		modelAndView.addObject("user", vo);
		
		log.info(userService.getCategories());
		
		modelAndView.addObject("categoryList", userService.getCategories());
		modelAndView.addObject("majorLength", userService.getCategories().size() - 1);
		
		List<CategoryVO> categoryVOs = userService.getUserCategories(userNo);
		
		
		modelAndView.addObject("userCategoryList", categoryVOs);
		log.info(categoryVOs);
		
		log.info(userService.getUserCategories(userNo).toString());
		
		List<AddressVO> userAddrList = userService.getUserAddr(userNo);
		
		log.info(userAddrList);
		if (userAddrList.size() != 0) {
			modelAndView.addObject("userAddrList", userAddrList);			
		}
		
		
		return modelAndView;
	}
	
	@GetMapping("/userBasket")
	public ModelAndView basket(ModelAndView modelAndView) {
		System.out.println("/userBasket GET");
		modelAndView.setViewName("redirect:/user/userMyPage/3");
		return modelAndView;
	}
	
	@GetMapping("/userMyPageBoardList")
	@ResponseBody
	public List<BoardVO> userBoardList(PageVO paging) {
		
		paging.setCpp(9);
		
		return boardService.myList(paging);
	}
	

	@ResponseBody
	@PostMapping("/checkId")
	public String checkId(@RequestBody String userId) {
		log.info(userId);
		
		if (userService.checkId(userId) == 0) {

			return "accepted";
		} else {

			return "duplicated";
		}
		
		
	}
	
	@ResponseBody
	@PostMapping("/checkNickname")
	public String checkNickname(@RequestBody String userNickname) {
		log.info(userNickname);
		
		if (userService.checkNickname(userNickname) == 0) {
			
			return "accepted";
		} else {
			
			return "duplicated";
		}
		
		
	}
	
	@ResponseBody
	@PostMapping("/pwModify")
	public String pwModify(@RequestBody List<String> passwords, HttpServletRequest request) {
		log.info(passwords);
		String userPw = passwords.get(0);
		String modiPw = passwords.get(1);
		String checkPw = passwords.get(2);
		log.info(userPw);
		log.info(modiPw);
		log.info(checkPw);
		
		HttpSession session = request.getSession();
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		
		Map<String, String> pwMap = new HashMap<String, String>();
		pwMap.put("userNo", Integer.toString(userNo));
		pwMap.put("userPw", userPw);
		pwMap.put("modiPw", modiPw);
		pwMap.put("checkPw", checkPw);
		
		if (userService.checkCurrPw(pwMap) == 1) {
			
			if (modiPw.equals(checkPw)) {
				userService.changePw(pwMap);
			}
			
			return "PwChanged";
		} else {
			
			return "wrongPw";
		}
		
	}
	
	@ResponseBody
	@PostMapping("/userUpdateConfirm")
	public String userUpdateConfirm(@RequestBody List<String> passwords, HttpServletRequest request) {
		String userPw = passwords.get(0);
		String checkPw = passwords.get(1);
		
		HttpSession session = request.getSession();
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		
		Map<String, String> pwMap = new HashMap<String, String>();
		pwMap.put("userNo", Integer.toString(userNo));
		pwMap.put("userPw", userPw);
		pwMap.put("checkPw", checkPw);
		
		return Integer.toString(userService.checkCurrPw(pwMap));
	}
	
	@PostMapping("/userUpdate")
	public ModelAndView userUpdate(UserVO userVO, AddressVO addressVO, CategoryVO  boardCategoryVO, ModelAndView modelAndView, MultipartFile profilePic) {
		log.info(userVO); // 수정된 부분 확인 후 - border color 바뀐거로 구분하는 법 생각하기. db 수정
		log.info(addressVO); // 수정된 부분 확인 후 db 수정
		log.info(boardCategoryVO); // 삭제된 부분 조회 후 삭제 처리 먼저, 추가된 부분 확인 후 db favorite 추가. 
		log.info(profilePic); // 기존 거 삭제하고 새로운  파일로 변경. filename null 체크
		
		/*
			SELECT * from(
			    SELECT ROWNUM rn, tbl.*
			    from (
			    select f.favorite_no, u.user_no, c.category_no , c.category_major_title, c.category_minor_title
			    from favorite f JOIN duck_user u on f.favorite_user_no = u.user_no
			                    JOIN category c on f.favorite_category_no = c.category_no
			    ORDER BY c.category_major_title, c.category_minor_title
			    )tbl
			)
			WHERE rn = 4; 
			rn은 data-count의 값을 받아서 쓸 수 있으면 됨. 
			추가 삭제 전에 먼저 조회해서  f.favorite_no 반환하고 favorite table에서 이 번호를 삭제하면 삭제 처리 될듯.
		*/
		
		modelAndView.setViewName("redirect:/user/userMyPage/1");
		
		return modelAndView;
	}
	
	@ResponseBody
	@PostMapping("/userDelete")
	public String userDelete(@RequestBody List<String> passwords) {
		String userPw = passwords.get(0);
		String checkPw = passwords.get(1);
		
		return Integer.toString(1);
	}
	
	@GetMapping("/userFindAccount")
	public void userFindAccount() {}
	
	@PostMapping("/userFindAccount")
	public String userFindAccount (String userName, String userEmail, Model model) {
		List<String> userIds = userService.findAccount(userName,userEmail);
		model.addAttribute("userIds", userIds);
		return "/user/userFindAccountRes";
	}
	
	@GetMapping("/userFindAccountRes")
	public void successFindAccount() {}

	@GetMapping("/userFindPw")
	public void userFindPw() {}
	
	@PostMapping("/userFindPw")
	public String userFindPw(String userId, String userEmail) {
		// mailService에서 임시비밀번호 보내기
		String tmpPw = mailService.sendTmpPw(userEmail);
		// mapper에서 임시비밀번호로 비밀번호 수정하기
		userService.updatePw(userId, tmpPw);
		return "/user/userLogin";
	}
	
	@ResponseBody
	@GetMapping("/getProfile")
	public ResponseEntity<byte[]> getProfile(HttpSession session) {
		
		int userNo = ((UserVO) session.getAttribute("login")).getUserNo();
		UserVO loginUser = userService.getUserVoWithNo(userNo);
		
		File file = new File(loginUser.getUserProfilePath() + loginUser.getUserProfileFolder() + '/' +loginUser.getUserProfileFileName());
		ResponseEntity<byte[]> result = null;
		HttpHeaders headers = new HttpHeaders();
		try {
			headers.add("Content-Type", Files.probeContentType(file.toPath()));
			result = new ResponseEntity<byte[]>(FileCopyUtils.copyToByteArray(file), headers, HttpStatus.OK);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	
	

}
