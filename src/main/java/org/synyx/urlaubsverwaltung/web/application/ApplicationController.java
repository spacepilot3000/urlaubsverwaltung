package org.synyx.urlaubsverwaltung.web.application;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.joda.time.DateMidnight;
import org.joda.time.chrono.GregorianChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;

import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.synyx.urlaubsverwaltung.DateFormat;
import org.synyx.urlaubsverwaltung.core.account.Account;
import org.synyx.urlaubsverwaltung.core.account.AccountService;
import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationStatus;
import org.synyx.urlaubsverwaltung.core.application.domain.Comment;
import org.synyx.urlaubsverwaltung.core.application.domain.OverlapCase;
import org.synyx.urlaubsverwaltung.core.application.domain.VacationType;
import org.synyx.urlaubsverwaltung.core.application.service.ApplicationInteractionService;
import org.synyx.urlaubsverwaltung.core.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.core.application.service.CalculationService;
import org.synyx.urlaubsverwaltung.core.application.service.CommentService;
import org.synyx.urlaubsverwaltung.core.application.service.OverlapService;
import org.synyx.urlaubsverwaltung.core.mail.MailService;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.person.PersonService;
import org.synyx.urlaubsverwaltung.core.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.core.sicknote.statistics.SickNoteStatistics;
import org.synyx.urlaubsverwaltung.core.util.CalcUtil;
import org.synyx.urlaubsverwaltung.core.util.DateUtil;
import org.synyx.urlaubsverwaltung.security.Role;
import org.synyx.urlaubsverwaltung.security.SessionService;
import org.synyx.urlaubsverwaltung.web.ControllerConstants;
import org.synyx.urlaubsverwaltung.web.person.PersonConstants;
import org.synyx.urlaubsverwaltung.web.sicknote.FilterRequest;
import org.synyx.urlaubsverwaltung.web.sicknote.PersonPropertyEditor;
import org.synyx.urlaubsverwaltung.web.util.DateMidnightPropertyEditor;
import org.synyx.urlaubsverwaltung.web.util.GravatarUtil;
import org.synyx.urlaubsverwaltung.web.validator.ApplicationValidator;
import org.synyx.urlaubsverwaltung.web.validator.CommentValidator;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Controller for management of {@link Application}s.
 *
 * @author  Aljona Murygina
 */
@RequestMapping("/application")
@Controller
public class ApplicationController {

    @Autowired
    private PersonService personService;

    @Autowired
    private ApplicationInteractionService applicationInteractionService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private OverlapService overlapService;

    @Autowired
    private CalculationService calculationService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ApplicationValidator applicationValidator;

    @Autowired
    private CommentValidator commentValidator;

    @Autowired
    private MailService mailService;

    @Autowired
    private SessionService sessionService;

    @InitBinder
    public void initBinder(DataBinder binder, Locale locale) {

        binder.registerCustomEditor(DateMidnight.class, new DateMidnightPropertyEditor(locale));
        binder.registerCustomEditor(Person.class, new PersonPropertyEditor(personService));
    }


    /**
     * Show waiting applications for leave on default.
     *
     * @return  waiting applications for leave page or error page if not boss or office
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String showDefault() {

        if (sessionService.isBoss() || sessionService.isOffice()) {
            return "redirect:/web" + "/" + "application" + "/waiting";
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    /**
     * Show all applications for leave, not dependent on their status.
     *
     * @param  year  if not given, the current year is used to display applications for leave for
     * @param  model
     *
     * @return  all applications for leave for the given year or for the current year if no year is given
     */
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public String showAll(@RequestParam(value = ControllerConstants.YEAR, required = false) Integer year, Model model) {

        if (sessionService.isBoss() || sessionService.isOffice()) {
            int yearToDisplay = year == null ? DateMidnight.now().getYear() : year;

            List<Application> applicationsForLeave = getAllRelevantApplicationsForLeave(yearToDisplay);

            model.addAttribute(ControllerConstants.APPLICATIONS, applicationsForLeave);
            model.addAttribute(PersonConstants.GRAVATAR_URLS, getAllRelevantGravatarUrls(applicationsForLeave));
            model.addAttribute(PersonConstants.LOGGED_USER, sessionService.getLoggedUser());
            model.addAttribute("titleApp", "applications.all");
            model.addAttribute(ControllerConstants.YEAR, DateMidnight.now().getYear());
            model.addAttribute("filterRequest", new FilterRequest());

            return ControllerConstants.APPLICATION + "/app_list";
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    /**
     * Get all relevant applications for leave, i.e. not cancelled applications for leave and cancelled but formerly
     * allowed applications for leave.
     *
     * @param  year  to get applications for leave for
     *
     * @return  all relevant applications for leave
     */
    private List<Application> getAllRelevantApplicationsForLeave(int year) {

        DateMidnight firstDay = DateUtil.getFirstDayOfYear(year);
        DateMidnight lastDay = DateUtil.getLastDayOfYear(year);

        List<Application> applications = applicationService.getApplicationsForACertainPeriod(firstDay, lastDay);

        List<Application> filteredApplications = Lists.newArrayList(Iterables.filter(applications,
                    new Predicate<Application>() {

                        @Override
                        public boolean apply(Application application) {

                            boolean isNotCancelled = !application.hasStatus(ApplicationStatus.CANCELLED);
                            boolean isCancelledButWasAllowed = application.hasStatus(ApplicationStatus.CANCELLED)
                                && application.isFormerlyAllowed();

                            return isNotCancelled || isCancelledButWasAllowed;
                        }
                    }));

        return filteredApplications;
    }


    /**
     * Get all gravatar urls for the persons of the given applications for leave.
     *
     * @param  applications  of the persons for that gravatar urls should be fetched for
     *
     * @return  gravatar urls mapped to applications for leave
     */
    private Map<Application, String> getAllRelevantGravatarUrls(List<Application> applications) {

        Map<Application, String> gravatarUrls = new HashMap<>();

        for (Application application : applications) {
            String gravatarUrl = GravatarUtil.createImgURL(application.getPerson().getEmail());

            if (gravatarUrl != null) {
                gravatarUrls.put(application, gravatarUrl);
            }
        }

        return gravatarUrls;
    }


    /**
     * Show waiting applications for leave.
     *
     * @param  year  if not given, the current year is used to display applications for leave for
     * @param  model
     *
     * @return  waiting applications for leave for the given year or for the current year if no year is given
     */
    @RequestMapping(value = "/waiting", method = RequestMethod.GET)
    public String showWaiting(@RequestParam(value = ControllerConstants.YEAR, required = false) Integer year,
        Model model) {

        if (sessionService.isBoss() || sessionService.isOffice()) {
            int yearToDisplay = year == null ? DateMidnight.now().getYear() : year;

            return prepareRelevantApplicationsForLeave(ApplicationStatus.WAITING, yearToDisplay, model);
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    private String prepareRelevantApplicationsForLeave(ApplicationStatus status, int year, Model model) {

        String title = "";

        if (status == ApplicationStatus.WAITING) {
            title = "applications.waiting";
        } else if (status == ApplicationStatus.ALLOWED) {
            title = "applications.allowed";
        } else if (status == ApplicationStatus.CANCELLED) {
            title = "applications.cancelled";
        } else if (status == ApplicationStatus.REJECTED) {
            title = "applications.rejected";
        }

        DateMidnight firstDay = DateUtil.getFirstDayOfYear(year);
        DateMidnight lastDay = DateUtil.getLastDayOfYear(year);

        List<Application> applicationsToBeShown;

        List<Application> applications = applicationService.getApplicationsForACertainPeriodAndState(firstDay, lastDay,
                status);

        // only formerly allowed applications for leave are relevant for cancelled status
        if (status == ApplicationStatus.CANCELLED) {
            applicationsToBeShown = Lists.newArrayList(Iterables.filter(applications, new Predicate<Application>() {

                            @Override
                            public boolean apply(Application application) {

                                return application.isFormerlyAllowed();
                            }
                        }));
        } else {
            applicationsToBeShown = applications;
        }

        Map<Application, String> gravatarUrls = getAllRelevantGravatarUrls(applicationsToBeShown);

        model.addAttribute(PersonConstants.GRAVATAR_URLS, gravatarUrls);
        model.addAttribute(ControllerConstants.APPLICATIONS, applicationsToBeShown);
        model.addAttribute(PersonConstants.LOGGED_USER, sessionService.getLoggedUser());
        model.addAttribute("titleApp", title);
        model.addAttribute(ControllerConstants.YEAR, DateMidnight.now().getYear());
        model.addAttribute("filterRequest", new FilterRequest());

        return ControllerConstants.APPLICATION + "/app_list";
    }


    /**
     * Show allowed applications for leave.
     *
     * @param  year  if not given, the current year is used to display applications for leave for
     * @param  model
     *
     * @return  allowed applications for leave for the given year or for the current year if no year is given
     */
    @RequestMapping(value = "/allowed", method = RequestMethod.GET)
    public String showAllowed(@RequestParam(value = ControllerConstants.YEAR, required = false) Integer year,
        Model model) {

        if (sessionService.isBoss() || sessionService.isOffice()) {
            int yearToDisplay = year == null ? DateMidnight.now().getYear() : year;

            return prepareRelevantApplicationsForLeave(ApplicationStatus.ALLOWED, yearToDisplay, model);
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    /**
     * Show cancelled applications for leave.
     *
     * @param  year  if not given, the current year is used to display applications for leave for
     * @param  model
     *
     * @return  cancelled applications for leave for the given year or for the current year if no year is given
     */
    @RequestMapping(value = "/cancelled", method = RequestMethod.GET)
    public String showCancelled(@RequestParam(value = ControllerConstants.YEAR, required = false) Integer year,
        Model model) {

        if (sessionService.isBoss() || sessionService.isOffice()) {
            int yearToDisplay = year == null ? DateMidnight.now().getYear() : year;

            return prepareRelevantApplicationsForLeave(ApplicationStatus.CANCELLED, yearToDisplay, model);
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    /**
     * Show rejected applications for leave.
     *
     * @param  year  if not given, the current year is used to display applications for leave for
     * @param  model
     *
     * @return  rejected applications for leave for the given year or for the current year if no year is given
     */
    @RequestMapping(value = "/rejected", method = RequestMethod.GET)
    public String showRejected(@RequestParam(value = ControllerConstants.YEAR, required = false) Integer year,
        Model model) {

        if (sessionService.isBoss() || sessionService.isOffice()) {
            int yearToDisplay = year == null ? DateMidnight.now().getYear() : year;

            return prepareRelevantApplicationsForLeave(ApplicationStatus.REJECTED, yearToDisplay, model);
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    /**
     * Show form to apply an application for leave.
     *
     * @param  model
     *
     * @return  form to apply an application for leave
     */
    @RequestMapping(value = "/new", method = RequestMethod.GET)
    public String newApplicationForm(Model model) {

        if (sessionService.isInactive()) {
            return ControllerConstants.ERROR_JSP;
        } else {
            Person person = sessionService.getLoggedUser();
            Account holidaysAccount = accountService.getHolidaysAccount(DateMidnight.now().getYear(), person);

            if (holidaysAccount == null) {
                model.addAttribute("notpossible", true);
            } else {
                prepareApplicationForLeaveForm(person, new AppForm(), model);
            }

            return ControllerConstants.APPLICATION + "/app_form";
        }
    }


    private void prepareApplicationForLeaveForm(Person person, AppForm appForm, Model model) {

        List<Person> persons = personService.getAllPersonsExcept(person);

        Account account = accountService.getHolidaysAccount(DateMidnight.now(GregorianChronology.getInstance())
                .getYear(), person);

        if (account != null) {
            BigDecimal vacationDaysLeft = calculationService.calculateLeftVacationDays(account);
            BigDecimal remainingVacationDaysLeft = calculationService.calculateLeftRemainingVacationDays(account);
            model.addAttribute(PersonConstants.LEFT_DAYS, vacationDaysLeft);
            model.addAttribute(PersonConstants.REM_LEFT_DAYS, remainingVacationDaysLeft);
            model.addAttribute(PersonConstants.BEFORE_APRIL, DateUtil.isBeforeApril(DateMidnight.now()));
        }

        model.addAttribute(ControllerConstants.PERSON, person);
        model.addAttribute(ControllerConstants.PERSONS, persons);
        model.addAttribute("date", DateMidnight.now(GregorianChronology.getInstance()));
        model.addAttribute(ControllerConstants.YEAR, DateMidnight.now(GregorianChronology.getInstance()).getYear());
        model.addAttribute("appForm", appForm);
        model.addAttribute(ControllerConstants.ACCOUNT, account);
        model.addAttribute("vacTypes", VacationType.values());
        model.addAttribute(PersonConstants.LOGGED_USER, sessionService.getLoggedUser());
    }


    /**
     * Apply an application for leave. The application will have waiting state after applying. If applying was
     * successful the details to the created application for leave will be displayed.
     *
     * @param  appForm
     * @param  errors
     * @param  model
     *
     * @return  the details page of the created application for leave if everything was successful or the validated
     *          application for leave form
     */
    @RequestMapping(value = "/new", method = RequestMethod.POST)
    public String newApplicationByUser(@ModelAttribute("appForm") AppForm appForm, Errors errors, Model model) {

        return newApplication(sessionService.getLoggedUser(), appForm, false, errors, model);
    }


    private String newApplication(Person person, AppForm appForm, boolean isOffice, Errors errors, Model model) {

        Person personForForm;

        if (isOffice) {
            personForForm = person;
        } else {
            personForForm = sessionService.getLoggedUser();
        }

        applicationValidator.validate(appForm, errors);

        if (errors.hasErrors()) {
            prepareApplicationForLeaveForm(personForForm, appForm, model);

            if (errors.hasGlobalErrors()) {
                model.addAttribute("errors", errors);
            }

            return ControllerConstants.APPLICATION + "/app_form";
        }

        if (checkAndSaveApplicationForm(appForm, errors)) {
            int id = applicationService.getIdOfLatestApplication(personForForm, ApplicationStatus.WAITING);

            return "redirect:/web/application/" + id;
        } else {
            prepareApplicationForLeaveForm(personForForm, appForm, model);

            if (errors.hasGlobalErrors()) {
                model.addAttribute("errors", errors);
            }

            return ControllerConstants.APPLICATION + "/app_form";
        }
    }


    /**
     * This method checks if there are overlapping applications and if the user has enough vacation days to apply for
     * leave.
     *
     * @param  appForm
     * @param  errors
     *
     * @return  true if everything is alright and application can be saved, else false
     */
    private boolean checkAndSaveApplicationForm(AppForm appForm, Errors errors) {

        Application application = appForm.createApplicationObject();

        BigDecimal days = applicationInteractionService.getNumberOfVacationDays(application);

        // ensure that no one applies for leave for a vacation of 0 days
        if (CalcUtil.isZero(days)) {
            errors.reject("check.zero");

            return false;
        }

        // check at first if there are existent application for the same period

        // checkOverlap
        // case 1: ok
        // case 2: new application is fully part of existent applications, useless to apply it
        // case 3: gaps in between - feature in later version, now only error message

        OverlapCase overlap = overlapService.checkOverlap(application);

        boolean isOverlapping = overlap == OverlapCase.FULLY_OVERLAPPING || overlap == OverlapCase.PARTLY_OVERLAPPING;

        if (isOverlapping) {
            // in this version, these two cases are handled equal
            errors.reject("check.overlap");

            return false;
        }

        // if there is no overlap go to next check but only if vacation type is holiday, else you don't have to
        // check if there are enough days on user's holidays account
        boolean enoughDays = false;
        boolean isHoliday = application.getVacationType() == VacationType.HOLIDAY;

        if (isHoliday) {
            enoughDays = calculationService.checkApplication(application);
        }

        boolean mayApplyForLeave = (isHoliday && enoughDays) || !isHoliday;

        if (mayApplyForLeave) {
            applicationInteractionService.apply(application, sessionService.getLoggedUser());

            return true;
        } else {
            errors.reject("check.enough");

            return false;
        }
    }


    /**
     * This method is analogue to application for leave form for user, but office is able to apply for leave on behalf
     * of other users.
     *
     * @param  personId  to apply leave for
     * @param  model
     *
     * @return
     */
    @RequestMapping(value = "/new", params = "personId", method = RequestMethod.GET)
    public String newApplicationFormForOffice(@RequestParam("personId") Integer personId, Model model) {

        // only office may apply for leave on behalf of other users
        if (sessionService.isOffice()) {
            Person person = personService.getPersonByID(personId);

            // check if the logged user is active
            if (sessionService.isInactive()) {
                model.addAttribute("notpossible", true);
            } else {
                // check if the logged user has a current/valid holidays account
                if (accountService.getHolidaysAccount(DateMidnight.now().getYear(), person) == null) {
                    model.addAttribute("notpossible", true);
                } else {
                    prepareApplicationForLeaveForm(person, new AppForm(), model);

                    List<Person> persons = personService.getActivePersons();
                    model.addAttribute("personList", persons);
                }
            }

            return ControllerConstants.APPLICATION + "/app_form";
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    /**
     * Apply an application for leave on behalf of an user. The application will have waiting state after applying. If
     * applying was successful the details to the created application for leave will be displayed.
     *
     * <p>NOTE: Can not use 'person' as param, must be 'personId' to differ from field
     * {@link org.synyx.urlaubsverwaltung.web.application.AppForm#person}. Else you get errors in property binding.</p>
     *
     * @param  personId  person on behalf application is applied
     * @param  appForm
     * @param  errors
     * @param  model
     *
     * @return  the details page of the created application for leave if everything was successful or the validated
     *          application for leave form*
     */
    @RequestMapping(value = "/new", params = "personId", method = RequestMethod.POST)
    public String newApplicationByOffice(@RequestParam("personId") Integer personId,
        @ModelAttribute("appForm") AppForm appForm, Errors errors, Model model) {

        Person person = personService.getPersonByID(personId);

        return newApplication(person, appForm, true, errors, model);
    }


    /**
     * Show detailled information to an application for leave.
     *
     * @param  applicationId  of the application for leave to show information for
     * @param  model
     *
     * @return  details page of the specific application for leave
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.GET)
    public String showApplicationDetail(@PathVariable("applicationId") Integer applicationId, Model model) {

        Person loggedUser = sessionService.getLoggedUser();

        Application application = applicationService.getApplicationById(applicationId);

        if (sessionService.isBoss() || sessionService.isOffice()) {
            prepareDetailView(application, model);

            return ControllerConstants.APPLICATION + "/app_detail";
        } else if (loggedUser.equals(application.getPerson())) {
            prepareDetailView(application, model);

            return ControllerConstants.APPLICATION + "/app_detail";
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    private void prepareDetailView(Application application, Model model) {

        model.addAttribute("comment", new Comment());

        List<Comment> comments = commentService.getCommentsByApplication(application);

        model.addAttribute("comments", comments);

        Map<Comment, String> gravatarUrls = new HashMap<>();

        for (Comment comment : comments) {
            String gravatarUrl = GravatarUtil.createImgURL(comment.getPerson().getEmail());

            if (gravatarUrl != null) {
                gravatarUrls.put(comment, gravatarUrl);
            }
        }

        model.addAttribute(PersonConstants.GRAVATAR_URLS, gravatarUrls);

        if (application.getStatus() == ApplicationStatus.WAITING && sessionService.isBoss()) {
            // get all persons that have the Boss Role
            List<Person> bosses = personService.getPersonsByRole(Role.BOSS);
            model.addAttribute("bosses", bosses);
            model.addAttribute("modelPerson", new Person());
        }

        model.addAttribute(PersonConstants.LOGGED_USER, sessionService.getLoggedUser());
        model.addAttribute("application", application);

        int year = application.getStartDate().getYear();

        Account account = accountService.getHolidaysAccount(year, application.getPerson());

        if (account != null) {
            BigDecimal vacationDaysLeft = calculationService.calculateLeftVacationDays(account);
            BigDecimal remainingVacationDaysLeft = calculationService.calculateLeftRemainingVacationDays(account);
            model.addAttribute(PersonConstants.LEFT_DAYS, vacationDaysLeft);
            model.addAttribute(PersonConstants.REM_LEFT_DAYS, remainingVacationDaysLeft);
            model.addAttribute(PersonConstants.BEFORE_APRIL, DateUtil.isBeforeApril(DateMidnight.now()));
        }

        model.addAttribute(ControllerConstants.ACCOUNT, account);
        model.addAttribute(ControllerConstants.YEAR, year);

        // get url of loggedUser's gravatar image
        String url = GravatarUtil.createImgURL(application.getPerson().getEmail());
        model.addAttribute("gravatar", url);
    }


    /**
     * Allow a not yet allowed application for leave (Boss only!).
     */
    @RequestMapping(value = "/{applicationId}/allow", method = RequestMethod.PUT)
    public String allowApplication(@PathVariable("applicationId") Integer applicationId,
        @ModelAttribute("comment") Comment comment, Errors errors, RedirectAttributes redirectAttributes) {

        Person boss = sessionService.getLoggedUser();
        Application application = applicationService.getApplicationById(applicationId);

        if (sessionService.isBoss()) {
            comment.setMandatory(false);
            commentValidator.validate(comment, errors);

            if (errors.hasErrors()) {
                redirectAttributes.addFlashAttribute("errors", errors);
                redirectAttributes.addFlashAttribute("action", "allow");
            } else {
                applicationInteractionService.allow(application, boss, comment);
                redirectAttributes.addFlashAttribute("allowSuccess", true);
            }

            return "redirect:/web/application/" + applicationId;
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    /**
     * If a boss is not sure about the decision if an application should be allowed or rejected, he can ask another boss
     * to decide about this application (an email is sent).
     *
     * @param  applicationId
     *
     * @return
     */
    @RequestMapping(value = "/{applicationId}/refer", method = RequestMethod.PUT)
    public String referApplication(@PathVariable("applicationId") Integer applicationId,
        @ModelAttribute("modelPerson") Person p, RedirectAttributes redirectAttributes) {

        Application application = applicationService.getApplicationById(applicationId);

        Person sender = sessionService.getLoggedUser();
        Person recipient = personService.getPersonByLogin(p.getLoginName());
        mailService.sendReferApplicationNotification(application, recipient, sender);

        redirectAttributes.addFlashAttribute("referSuccess", true);

        return "redirect:/web/application/" + applicationId;
    }


    /**
     * Reject an application for leave (Boss only!).
     */
    @RequestMapping(value = "/{applicationId}/reject", method = RequestMethod.PUT)
    public String rejectApplication(@PathVariable("applicationId") Integer applicationId,
        @ModelAttribute("comment") Comment comment, Errors errors, RedirectAttributes redirectAttributes) {

        Person boss = sessionService.getLoggedUser();

        if (sessionService.isBoss()) {
            Application application = applicationService.getApplicationById(applicationId);

            comment.setMandatory(true);
            commentValidator.validate(comment, errors);

            if (errors.hasErrors()) {
                redirectAttributes.addFlashAttribute("errors", errors);
                redirectAttributes.addFlashAttribute("action", "reject");
            } else {
                applicationInteractionService.reject(application, boss, comment);
                redirectAttributes.addFlashAttribute("rejectSuccess", true);
            }

            return "redirect:/web/application/" + applicationId;
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    /**
     * After confirming by user: this method set an application to cancelled.
     *
     * @param  applicationId
     *
     * @return
     */
    @RequestMapping(value = "/{applicationId}/cancel", method = RequestMethod.PUT)
    public String cancelApplication(@PathVariable("applicationId") Integer applicationId,
        @ModelAttribute("comment") Comment comment, Errors errors, RedirectAttributes redirectAttributes) {

        Application application = applicationService.getApplicationById(applicationId);
        Person loggedUser = sessionService.getLoggedUser();
        ApplicationStatus status = application.getStatus();

        boolean isWaiting = status == ApplicationStatus.WAITING;
        boolean isAllowed = status == ApplicationStatus.ALLOWED;

        // security check: only two cases where cancelling is possible
        // 1: office can cancel all applications for leave that has the state waiting or allowed, even for other persons
        // 2: user can cancel his own applications for leave if they have the state waiting
        boolean officeIsCancelling = sessionService.isOffice() && (isWaiting || isAllowed);
        boolean userIsCancelling = loggedUser.equals(application.getPerson()) && isWaiting;

        if (officeIsCancelling || userIsCancelling) {
            // user can cancel only his own waiting applications, so the comment is NOT mandatory
            if (userIsCancelling) {
                comment.setMandatory(false);
            }
            // office cancels application of other users, state can be waiting or allowed, so the comment is mandatory
            else if (officeIsCancelling) {
                comment.setMandatory(true);
            }

            commentValidator.validate(comment, errors);

            if (errors.hasErrors()) {
                redirectAttributes.addFlashAttribute("errors", errors);
                redirectAttributes.addFlashAttribute("action", "cancel");
            } else {
                applicationInteractionService.cancel(application, loggedUser, comment);
            }

            return "redirect:/web/application/" + applicationId;
        } else {
            return ControllerConstants.ERROR_JSP;
        }
    }


    @RequestMapping(value = "/{applicationId}/remind", method = RequestMethod.PUT)
    public String remindBoss(@PathVariable("applicationId") Integer applicationId,
        RedirectAttributes redirectAttributes) {

        // TODO: move this to a service method

        Application application = applicationService.getApplicationById(applicationId);
        DateMidnight remindDate = application.getRemindDate();

        if (remindDate != null) {
            if (remindDate.isEqualNow()) {
                redirectAttributes.addFlashAttribute("remindAlreadySent", true);
            }
        } else {
            int minDaysToWait = 2;

            DateMidnight minDateForNotification = application.getApplicationDate().plusDays(minDaysToWait);

            if (minDateForNotification.isAfterNow()) {
                redirectAttributes.addFlashAttribute("remindNoWay", true);
            } else {
                mailService.sendRemindBossNotification(application);
                application.setRemindDate(DateMidnight.now());
                applicationService.save(application);
                redirectAttributes.addFlashAttribute("remindIsSent", true);
            }
        }

        return "redirect:/web/application/" + applicationId;
    }


    @RequestMapping(value = "/statistics", method = RequestMethod.POST)
    public String applicationForLeaveStatistics(@ModelAttribute("filterRequest") FilterRequest filterRequest) {

        if (sessionService.isOffice()) {
            DateMidnight now = DateMidnight.now();
            DateMidnight from = now;
            DateMidnight to = now;

            if (filterRequest.getPeriod().equals(FilterRequest.Period.YEAR)) {
                from = now.dayOfYear().withMinimumValue();
                to = now.dayOfYear().withMaximumValue();
            } else if (filterRequest.getPeriod().equals(FilterRequest.Period.QUARTAL)) {
                from = now.dayOfMonth().withMinimumValue().minusMonths(2);

                // TODO: This is quickfix...
                if (from.getYear() != now.getYear()) {
                    from = now.dayOfYear().withMinimumValue();
                }

                to = now.dayOfMonth().withMaximumValue();
            } else if (filterRequest.getPeriod().equals(FilterRequest.Period.MONTH)) {
                from = now.dayOfMonth().withMinimumValue();
                to = now.dayOfMonth().withMaximumValue();
            }

            return "redirect:/web/application/statistics?from=" + from.toString(DateFormat.PATTERN) + "&to="
                + to.toString(DateFormat.PATTERN);
        }

        return ControllerConstants.ERROR_JSP;
    }


    @RequestMapping(value = "/statistics", method = RequestMethod.GET, params = { "from", "to" })
    public String applicationForLeaveStatistics(@RequestParam("from") String from,
        @RequestParam("to") String to, Model model) {

        if (sessionService.isOffice()) {
            DateTimeFormatter formatter = DateTimeFormat.forPattern(DateFormat.PATTERN);
            DateMidnight fromDate = DateMidnight.parse(from, formatter);
            DateMidnight toDate = DateMidnight.parse(to, formatter);

            List<Person> persons = personService.getActivePersons();

            Map<Person, String> gravatarUrls = new HashMap<>();

            Map<Person, BigDecimal> waitingVacationDays = new HashMap<>();
            Map<Person, BigDecimal> allowedVacationDays = new HashMap<>();

            Map<Person, BigDecimal> leftVacationDays = new HashMap<>();

            for (Person person : persons) {
                String gravatarUrl = GravatarUtil.createImgURL(person.getEmail());

                if (gravatarUrl != null) {
                    gravatarUrls.put(person, gravatarUrl);
                }

                Account account = accountService.getHolidaysAccount(fromDate.getYear(), person);

                if (account != null) {
                    BigDecimal vacationDaysLeft = calculationService.calculateTotalLeftVacationDays(account);
                    leftVacationDays.put(person, vacationDaysLeft);
                }

                List<Application> waitingApplications =
                    applicationService.getApplicationsForACertainPeriodAndPersonAndState(fromDate, toDate, person,
                        ApplicationStatus.WAITING);

                BigDecimal numberOfWaitingDays = BigDecimal.ZERO;

                for (Application waitingApplication : waitingApplications) {
                    // TODO: It's not so easy....days of application are not correct if the application spans two years
                    numberOfWaitingDays = numberOfWaitingDays.add(waitingApplication.getDays());
                }

                List<Application> allowedApplications =
                    applicationService.getApplicationsForACertainPeriodAndPersonAndState(fromDate, toDate, person,
                        ApplicationStatus.ALLOWED);

                BigDecimal numberOfAllowedDays = BigDecimal.ZERO;

                for (Application allowedApplication : allowedApplications) {
                    numberOfAllowedDays = numberOfAllowedDays.add(allowedApplication.getDays());
                }

                waitingVacationDays.put(person, numberOfWaitingDays);
                allowedVacationDays.put(person, numberOfAllowedDays);
            }

            model.addAttribute("from", fromDate);
            model.addAttribute("to", toDate);
            model.addAttribute(ControllerConstants.PERSONS, persons);
            model.addAttribute(PersonConstants.GRAVATAR_URLS, gravatarUrls);
            model.addAttribute(PersonConstants.LEFT_DAYS, leftVacationDays);
            model.addAttribute("waitingDays", waitingVacationDays);
            model.addAttribute("allowedDays", allowedVacationDays);
            model.addAttribute("filterRequest", new FilterRequest());

            return ControllerConstants.APPLICATION + "/app_statistics";
        }

        return ControllerConstants.ERROR_JSP;
    }
}
