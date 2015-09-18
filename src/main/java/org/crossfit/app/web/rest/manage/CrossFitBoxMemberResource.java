package org.crossfit.app.web.rest.manage;

import java.util.Optional;

import javax.inject.Inject;

import org.crossfit.app.domain.Member;
import org.crossfit.app.domain.User;
import org.crossfit.app.repository.AuthorityRepository;
import org.crossfit.app.repository.MemberRepository;
import org.crossfit.app.repository.UserRepository;
import org.crossfit.app.security.AuthoritiesConstants;
import org.crossfit.app.security.SecurityUtils;
import org.crossfit.app.service.CrossFitBoxSerivce;
import org.crossfit.app.service.MailService;
import org.crossfit.app.service.util.RandomUtil;
import org.crossfit.app.web.rest.MemberResource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Sets;

/**
 * REST controller for managing Member.
 */
@RestController
@RequestMapping("/manage")
public class CrossFitBoxMemberResource extends MemberResource {

    private final Logger log = LoggerFactory.getLogger(CrossFitBoxMemberResource.class);

    @Inject
    private MemberRepository memberRepository;
    @Inject
    private UserRepository userRepository;

    @Inject
    private CrossFitBoxSerivce boxService;

    @Inject
    private AuthorityRepository authorityRepository;
	
    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
	private MailService mailService;

	@Override
	protected Member doSave(Member member) {
		if (member.getId() == null){
			
			member.getUser().setAuthorities(Sets.newHashSet(authorityRepository.findOne(AuthoritiesConstants.USER)));
			member.getUser().setCreatedBy(((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
			member.getUser().setCreatedDate(DateTime.now());
			member.getUser().setLogin(member.getUser().getEmail());
			
			
			member.setBox(boxService.findCurrentCrossFitBox());
			

			initAccountAndSendMail(member);
		}
		else{
			//Les seuls champs modifiable de user, c'est le nom, le prénom & l'email
			String firstName = member.getUser().getFirstName();
			String lastName = member.getUser().getLastName();
			String email = member.getUser().getEmail();
			User user = userRepository.findOne(member.getUser().getId());
			user.setFirstName(firstName);
			user.setLastName(lastName);
			user.setLastModifiedBy(((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
			user.setLastModifiedDate(DateTime.now());
			
			member.setUser(user);		
			
			//L'email a changé ? on repasse par une validation d'email
			if (!email.equals(user.getEmail())){
				user.setEmail(email);
				user.setLogin(email);
				initAccountAndSendMail(member);
			}
		}

		
		return super.doSave(member);
	}

	protected void initAccountAndSendMail(Member member) {
		String generatePassword = RandomUtil.generatePassword();
		member.getUser().setPassword(passwordEncoder.encode(generatePassword));
		member.getUser().setActivated(false);
		member.getUser().setActivationKey(RandomUtil.generateActivationKey());

		mailService.sendActivationEmail(member.getUser(), generatePassword, member.getBox());
	}

	@Override
	protected Page<Member> doFindAll(Pageable generatePageRequest) {
		return memberRepository.findAll(boxService.findCurrentCrossFitBox(), generatePageRequest);
	}

	@Override
	protected Member doGet(Long id) {
		return memberRepository.findOne(id, boxService.findCurrentCrossFitBox());
	}

	@Override
	protected void doDelete(Long id) {
		Member memberToDelete = memberRepository.findOne(id);
		if (memberToDelete.getBox().equals(boxService.findCurrentCrossFitBox())){
			memberRepository.delete(memberToDelete);
		}
	}
    

	/**
	 * GET /members/logged -> get the current member.
	 */

	@RequestMapping(value = "/members/logged", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public ResponseEntity<Member> getCurrentMember() {
		log.debug("REST request to get current Member : {}", SecurityUtils.getCurrentLogin());
		
		
		return Optional.ofNullable(doGetCurrent())
				.map(member -> new ResponseEntity<>(member, HttpStatus.OK))
				.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}
    
	protected Member doGetCurrent() {
		return memberRepository.findOneByLogin(SecurityUtils.getCurrentLogin(), boxService.findCurrentCrossFitBox());
	}
	
}
