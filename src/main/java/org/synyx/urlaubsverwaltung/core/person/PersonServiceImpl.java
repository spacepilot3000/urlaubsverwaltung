package org.synyx.urlaubsverwaltung.core.person;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.synyx.urlaubsverwaltung.core.mail.MailNotification;
import org.synyx.urlaubsverwaltung.security.Role;

import java.util.List;


/**
 * Implementation for {@link PersonService}.
 *
 * @author  Aljona Murygina
 * @author  Johannes Reuter
 */
@Service("personService")
@Transactional
class PersonServiceImpl implements PersonService {

    private final PersonDAO personDAO;

    @Autowired
    public PersonServiceImpl(PersonDAO personDAO) {

        this.personDAO = personDAO;
    }

    @Override
    public void save(Person person) {

        personDAO.save(person);
    }


    @Override
    public Person getPersonByID(Integer id) {

        return personDAO.findOne(id);
    }


    @Override
    public Person getPersonByLogin(String loginName) {

        return personDAO.findByLoginName(loginName);
    }


    @Override
    public List<Person> getActivePersons() {

        return personDAO.findActive();
    }


    @Override
    public List<Person> getAllPersonsExcept(final Person person) {

        return Lists.newArrayList(Iterables.filter(getActivePersons(), new Predicate<Person>() {

                        @Override
                        public boolean apply(Person p) {

                            return !p.equals(person);
                        }
                    }));
    }


    @Override
    public List<Person> getInactivePersons() {

        return personDAO.findInactive();
    }


    @Override
    public List<Person> getPersonsByRole(final Role role) {

        return Lists.newArrayList(Iterables.filter(getActivePersons(), new Predicate<Person>() {

                        @Override
                        public boolean apply(Person person) {

                            return person.hasRole(role);
                        }
                    }));
    }


    @Override
    public List<Person> getPersonsWithNotificationType(final MailNotification notification) {

        return Lists.newArrayList(Iterables.filter(getActivePersons(), new Predicate<Person>() {

                        @Override
                        public boolean apply(Person person) {

                            return person.hasNotificationType(notification);
                        }
                    }));
    }
}
