<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>seaduck e ne</title>

<!--개인 디자인 추가-->
<link href="${pageContext.request.contextPath }/resources/css/userFindAccount.css"
	rel="stylesheet">
</head>
<body>
<%@ include file="../include/header.jsp" %>


	<section class="py-5">
	    <div class="container">
		    <div class="row">
				<div class="col-lg-12 col-md-12 col-sm-12" id="userJoin-wrap">
					<h2 class="find-account">아이디 찾기</h2>	 
				</div>
	    	</div>
	    </div>
	</section>
	
	
<%@ include file="../include/footer.jsp" %>
    
</body>
</html>