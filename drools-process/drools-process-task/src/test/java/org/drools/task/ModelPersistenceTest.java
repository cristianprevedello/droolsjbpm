package org.drools.task;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.commons.collections.map.HashedMap;
import org.drools.task.Attachment;
import org.drools.task.BooleanExpression;
import org.drools.task.Comment;
import org.drools.task.Deadline;
import org.drools.task.Deadlines;
import org.drools.task.Delegation;
import org.drools.task.Escalation;
import org.drools.task.Group;
import org.drools.task.I18NText;
import org.drools.task.Notification;
import org.drools.task.OrganizationalEntity;
import org.drools.task.PeopleAssignments;
import org.drools.task.Reassignment;
import org.drools.task.Status;
import org.drools.task.Task;
import org.drools.task.TaskData;
import org.drools.task.User;
import org.drools.task.service.TaskService;
import org.drools.task.utils.CollectionUtils;
import org.mvel.MVEL;
import org.mvel.ParserContext;
import org.mvel.compiler.ExpressionCompiler;
import org.mvel.util.MVELClassLoader;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import junit.framework.TestCase;

public class ModelPersistenceTest extends BaseTest {
    
    protected void setUp() throws Exception {
        super.setUp();                    
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testfullHibernateRoundtripWithAdditionalMVELCheck() throws Exception {

        Task task1 = new Task();
        task1.setPriority( 100 );

        PeopleAssignments peopleAssignments = new PeopleAssignments();
        task1.setPeopleAssignments( peopleAssignments );

        peopleAssignments.setTaskInitiator( users.get(  "darth" ) );

        List<OrganizationalEntity> potentialOwners = new ArrayList<OrganizationalEntity>();
        peopleAssignments.setPotentialOwners( potentialOwners );
        potentialOwners.add( users.get( "bobba") );

        potentialOwners.add( users.get( "jabba") );

        List<OrganizationalEntity> excludedOwners = new ArrayList<OrganizationalEntity>();
        peopleAssignments.setExcludedOwners( excludedOwners );
        excludedOwners.add( users.get( "dalai" ) );

        excludedOwners.add( users.get( "christoper" ) );

        List<OrganizationalEntity> stakeholders = new ArrayList<OrganizationalEntity>();
        peopleAssignments.setTaskStakeholders( stakeholders );
        stakeholders.add( users.get( "stuart" ) );
        stakeholders.add( users.get( "jane" ) );

        List<OrganizationalEntity> businessAdmin = new ArrayList<OrganizationalEntity>();
        peopleAssignments.setBusinessAdministrators( businessAdmin );
        businessAdmin.add( users.get( "peter" ) );
        businessAdmin.add( users.get( "steve" ) );

        List<OrganizationalEntity> recipients = new ArrayList<OrganizationalEntity>();
        peopleAssignments.setRecipients( recipients );
        recipients.add( users.get( "sly" ) );
        recipients.add( users.get( "liz" ) );

        TaskData taskData = new TaskData();
        task1.setTaskData( taskData );
        
        taskData.setActualOwner( users.get( "liz" ) );
        taskData.setCreatedBy( users.get( "sly" ) );

        taskData.setActivationTime( new Date( 10000000 ) );
        taskData.setCreatedOn( new Date( 10000000 ) );
        taskData.setExpirationTime( new Date( 10000000 ) );
        taskData.setStatus( Status.Created );

        List<Attachment> attachments = new ArrayList<Attachment>();
        taskData.setAttachments( attachments );

        Attachment attachment = new Attachment();
        attachment.setAccessType( AccessType.Inline );
        attachment.setAttachedAt( new Date( 10000000 ) );
        attachment.setAttachedBy( users.get( "liz" ) );
        attachment.setContentType( "text" );
        attachment.setName( "file.txt" );
        attachment.setSize( 5000);
        attachment.setContentId( 5 );
        attachments.add( attachment );

        attachment = new Attachment();
        attachment.setAccessType( AccessType.Url );
        attachment.setAttachedAt( new Date( 10000000 ) );
        attachment.setAttachedBy( users.get( "liz" ) );
        attachment.setContentType( "text" );
        attachment.setName( "file2.txt" );
        attachment.setSize( 500 );
        attachment.setContentId( 3 );
        attachments.add( attachment );

        List<Comment> comments = new ArrayList<Comment>();
        taskData.setComments( comments );
        Comment comment = new Comment();
        comment.setAddedBy( users.get( "peter" ) );
        comment.setAddedAt( new Date( 10000000 ) );
        comment.setText( "this is a short comment" );
        comments.add( comment );

        comment = new Comment();
        comment.setAddedBy( users.get( "steve" ) );
        comment.setAddedAt( new Date( 10000000 ) );
        comment.setText( "this is a loooooooooooooooooooooooooooooooooooooooooooooooong comment" );
        comments.add( comment );

        List<I18NText> names = new ArrayList<I18NText>();
        task1.setNames( names );
        List<I18NText> subjects = new ArrayList<I18NText>();
        task1.setSubjects( subjects );
        List<I18NText> descriptions = new ArrayList<I18NText>();
        task1.setDescriptions( descriptions );

        names.add( new I18NText( "en-UK",
                                 "This is my task name" ) );
        names.add( new I18NText( "en-DK",
                                 "Dies ist mein task Name" ) );

        subjects.add( new I18NText( "en-UK",
                                    "This is my task subject" ) );
        subjects.add( new I18NText( "en-DK",
                                    "Das ist mein task Thema" ) );

        descriptions.add( new I18NText( "en-UK",
                                        "This is my task description" ) );
        descriptions.add( new I18NText( "en-DK",
                                        "Das ist mein task Beschreibung" ) );

        Delegation delegation = new Delegation();
        task1.setDelegation( delegation );
        delegation.setAllowed( Allowed.PotentialOwners );

        List<OrganizationalEntity> delegates = new ArrayList<OrganizationalEntity>();
        delegation.setDelegates( delegates );
        delegates.add( groups.get( "crusaders" ) );
        delegates.add( groups.get( "knightsTempler" ) );

        Deadlines deadlines = new Deadlines();
        task1.setDeadlines( deadlines );

        List<Deadline> startDeadlines = new ArrayList<Deadline>();
        deadlines.setStartDeadlines( startDeadlines );
        Deadline deadline = new Deadline();
        deadline.setEscalated( true );
        startDeadlines.add( deadline );
        deadline.setDate( new Date( 10000000 ) );
        List<I18NText> docs = new ArrayList<I18NText>();
        deadline.setDocumentation( docs );
        docs.add( new I18NText( "en-UK",
                                "Start Deadline documentation" ) );
        docs.add( new I18NText( "en-DK",
                                "Start Termin Dokumentation" ) );

        List<Escalation> escalations = new ArrayList<Escalation>();
        deadline.setEscalations( escalations );
        Escalation escalation = new Escalation();
        escalations.add( escalation );
        escalation.setName( "My Start Escalation" );

        List<BooleanExpression> constraints = new ArrayList<BooleanExpression>();
        escalation.setConstraints( constraints );
        constraints.add( new BooleanExpression( "mvel",
                                                "true" ) );

        List<Notification> notifications = new ArrayList<Notification>();
        escalation.setNotifications( notifications );

        Notification notification = new Notification();
        notifications.add( notification );
        notification.setPriority( 1000 );
        docs = new ArrayList<I18NText>();
        notification.setDocumentation( docs );
        docs.add( new I18NText( "en-UK",
                                "Start Notification documentation" ) );
        docs.add( new I18NText( "en-DK",
                                "Start Anmeldung Dokumentation" ) );

        businessAdmin = new ArrayList<OrganizationalEntity>();
        notification.setBusinessAdministrators( businessAdmin );
        businessAdmin.add( users.get( "bruce" ) );
        businessAdmin.add( users.get( "peter" ) );

        recipients = new ArrayList<OrganizationalEntity>();
        notification.setRecipients( recipients );
        recipients.add( users.get( "tony" ) );
        recipients.add( users.get( "darth" ) );

        names = new ArrayList<I18NText>();
        notification.setNames( names );
        subjects = new ArrayList<I18NText>();
        notification.setSubjects( subjects );
        descriptions = new ArrayList<I18NText>();
        notification.setDescriptions( descriptions );

        names.add( new I18NText( "en-UK",
                                 "This is my start notification name" ) );
        names.add( new I18NText( "en-DK",
                                 "Dies ist mein start anmeldung Name" ) );

        subjects.add( new I18NText( "en-UK", "This is my start notification subject" ) );
        subjects.add( new I18NText( "en-DK", "Das ist mein start anmeldung Thema" ) );

        descriptions.add( new I18NText( "en-UK", "This is my start notification description" ) );
        descriptions.add( new I18NText( "en-DK", "Das ist mein start anmeldung Beschreibung" ) );

        List<Reassignment> reassignments = new ArrayList<Reassignment>();
        escalation.setReassignments( reassignments );
        Reassignment reassignment = new Reassignment();
        reassignments.add( reassignment );

        docs = new ArrayList<I18NText>();
        reassignment.setDocumentation( docs );
        docs.add( new I18NText( "en-UK", "Start Reassignment documentation" ) );
        docs.add( new I18NText( "en-DK", "Start Neuzuweisung Dokumentation" ) );

        potentialOwners = new ArrayList<OrganizationalEntity>();
        reassignment.setPotentialOwners( potentialOwners );
        potentialOwners.add( users.get( "bobba" ) );
        potentialOwners.add( users.get( "luke" ) );
        
        List<Deadline> endDeadlines = new ArrayList<Deadline>();
        deadlines.setEndDeadlines( endDeadlines );
        deadline = new Deadline();
        deadline.setEscalated( true );
        endDeadlines.add( deadline );
        deadline.setDate( new Date( 10000000 ) );
        docs = new ArrayList<I18NText>();
        deadline.setDocumentation( docs );
        docs.add( new I18NText( "en-UK",
                                "End Deadline documentation" ) );
        docs.add( new I18NText( "en-DK",
                                "Ende Termin Dokumentation" ) );

        escalations = new ArrayList<Escalation>();
        deadline.setEscalations( escalations );
        escalation = new Escalation();
        escalations.add( escalation );
        escalation.setName( "My End Escalation" );

        constraints = new ArrayList<BooleanExpression>();
        escalation.setConstraints( constraints );
        constraints.add( new BooleanExpression( "mvel",
                                                "true" ) );

        notifications = new ArrayList<Notification>();
        escalation.setNotifications( notifications );

        notification = new Notification();
        notifications.add( notification );
        notification.setPriority( 1000 );
        docs = new ArrayList<I18NText>();
        notification.setDocumentation( docs );
        docs.add( new I18NText( "en-UK",
                                "End Notification documentation" ) );
        docs.add( new I18NText( "en-DK",
                                "Ende Anmeldung Dokumentation" ) );

        businessAdmin = new ArrayList<OrganizationalEntity>();
        notification.setBusinessAdministrators( businessAdmin );
        businessAdmin.add( users.get( "bobba" ) );
        businessAdmin.add( users.get( "darth" ) );

        recipients = new ArrayList<OrganizationalEntity>();
        notification.setRecipients( recipients );
        recipients.add( users.get( "liz" ) );
        recipients.add( users.get( "jane" ) );

        names = new ArrayList<I18NText>();
        notification.setNames( names );
        subjects = new ArrayList<I18NText>();
        notification.setSubjects( subjects );
        descriptions = new ArrayList<I18NText>();
        notification.setDescriptions( descriptions );

        names.add( new I18NText( "en-UK",
                                 "This is my end notification name" ) );
        names.add( new I18NText( "en-DK",
                                 "Dies ist mein ende anmeldung Name" ) );

        subjects.add( new I18NText( "en-UK", "This is my end notification subject" ) );
        subjects.add( new I18NText( "en-DK", "Das ist mein ende anmeldung Thema" ) );

        descriptions.add( new I18NText( "en-UK", "This is my end notification description" ) );
        descriptions.add( new I18NText( "en-DK", "Das ist mein ende anmeldung Beschreibung" ) );

        reassignments = new ArrayList<Reassignment>();
        escalation.setReassignments( reassignments );
        reassignment = new Reassignment();
        reassignments.add( reassignment );

        docs = new ArrayList<I18NText>();
        reassignment.setDocumentation( docs );
        docs.add( new I18NText( "en-UK", "End Reassignment documentation" ) );
        docs.add( new I18NText( "en-DK", "Ende Neuzuweisung Dokumentation" ) );

        potentialOwners = new ArrayList<OrganizationalEntity>();
        reassignment.setPotentialOwners( potentialOwners );
        potentialOwners.add( users.get( "stuart" ) );
        potentialOwners.add( users.get( "dalai" ) );        

        taskService.addTask( task1 );                
        
        taskService.getEntityManager().clear();
        
        Task task2 = taskService.getTask( task1.getId( ) );       
        
        assertNotSame( task1,
                       task2 );
        assertEquals( task1,
                      task2 );
        
        Reader reader = new InputStreamReader( getClass().getResourceAsStream( "FullyPopulatedTask.mvel" ) );
        Map  vars = new HashedMap();
        vars.put( "users", users );
        vars.put( "groups", groups );          
        vars.put( "bytes1", new byte[]{1, 0, 0, 1} );        
        Task task3= (Task) eval( reader, vars );               
        
        assertNotSame( task1,
                       task3 );
        assertEquals( task1,
                      task3 );
    }    

}
