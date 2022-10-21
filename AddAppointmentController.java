/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Contact;
import model.JDBC;

/**
 * FXML Controller class
 *
 * @author LabUser
 */
public class AddAppointmentController implements Initializable {

    @FXML private TextField titleField;
    @FXML private TextField descField;
    @FXML private TextField locationField;
    @FXML private TextField custID;
    @FXML private TextField userID;
    
    @FXML private DatePicker startField;
    @FXML private DatePicker endField;
    
    @FXML private ComboBox<String> type;
    @FXML private ComboBox<Contact> contact;
    @FXML private ComboBox<String> startHour,startMin;
    @FXML private ComboBox<String> endHour,endMin;
    
    private final ObservableList<Contact> contacts = FXCollections.observableArrayList();
    private final ObservableList<String> types = FXCollections.observableArrayList();
    private final ObservableList<String> hours = FXCollections.observableArrayList();
    private final ObservableList<String> minutes = FXCollections.observableArrayList();
    
    private Connection c;
    private ResultSet rs;
    private PreparedStatement pst;
    
    /**
     * This method saves appointment information
     * inputted by a user. 
     * 
     * @param event
     * @throws Exception 
     */
    public void saveAppoint(ActionEvent event) throws Exception {
     
        try {
            int appointID = generateID();
            String title = titleField.getText();
            String desc = descField.getText();
            String location = locationField.getText();
            String typeField = type.getValue();
            int cID = Integer.parseInt(custID.getText());
            int uID = Integer.parseInt(userID.getText()); 

            Contact choice = contact.getSelectionModel().getSelectedItem();
            int contactID = choice.getContactID();

            LocalDate startDate = startField.getValue();
            LocalDate endDate = endField.getValue();

            String sHour = startHour.getValue();
            String sMin = startMin.getValue();

            String eHour = endHour.getValue();
            String eMin = endMin.getValue();

            LocalDateTime startDateTime = LocalDateTime.of(startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(), Integer.parseInt(sHour), Integer.parseInt(sMin));
            LocalDateTime endDateTime = LocalDateTime.of(endDate.getYear(), endDate.getMonthValue(), endDate.getDayOfMonth(), Integer.parseInt(eHour), Integer.parseInt(eMin));      

            ZonedDateTime startZoneTime = ZonedDateTime.of(startDateTime, ZoneId.systemDefault());
            ZonedDateTime endZoneTime = ZonedDateTime.of(endDateTime, ZoneId.systemDefault());

            ZonedDateTime startZoneUtc = startZoneTime.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endZoneUtc = endZoneTime.withZoneSameInstant(ZoneOffset.UTC);

            LocalDateTime startLocalUtc = startZoneUtc.toLocalDateTime();
            LocalDateTime endLocalUtc = endZoneUtc.toLocalDateTime();

            Timestamp startStamp = Timestamp.valueOf(startLocalUtc);
            Timestamp endStamp = Timestamp.valueOf(endLocalUtc);

            String s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startStamp);
            String e = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endStamp);

            Timestamp formatStartStamp = Timestamp.valueOf(s); // Final value for database
            Timestamp formatEndStamp = Timestamp.valueOf(e); // Final Value for database

            boolean check = checkTimes(startDate, endDate, startZoneTime, endZoneTime);

            if(check == false) {     

                pst = c.prepareStatement("INSERT INTO appointments(Appointment_ID, Title, Description, Location, Type, Start, End, Customer_Id, User_ID, Contact_ID) "
                                                 + "VALUES(" + appointID + ", "
                                                   + "'" + title + "'" + ", "
                                                   + "'" + desc + "'" + ", "
                                                   + "'" + location + "'" + ", "
                                                   + "'" + typeField + "'" + ", "
                                                   + "'" + formatStartStamp + "'" + ", "
                                                   + "'" + formatEndStamp + "'" + ", "
                                                   + cID + ", "
                                                   + uID + ", "
                                                   + contactID + ")");

                    pst.execute();

                    Stage stage = (Stage)((Button)event.getSource()).getScene().getWindow();
                    Parent scene = FXMLLoader.load(getClass().getResource("/view/AppointmentForm.fxml"));
                    stage.setScene(new Scene(scene));
                    stage.show();
            }

            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Appointment times cannot overlap! Start times cannot come after end times!");
                alert.showAndWait();
            }
        }
        catch(NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Fill in all required information!");
            alert.showAndWait(); 
        }
        catch(SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Customer with this id does not exist!");
            alert.showAndWait(); 
        }
    }
    
    /**
     * This method checks for appointment overlap times.
     * 
     * @param s
     * @param e
     * @param zoneS
     * @param zoneE
     * @return true or false
     * @throws SQLException 
     */
    public boolean checkTimes(LocalDate s, LocalDate e, ZonedDateTime zoneS, ZonedDateTime zoneE) throws SQLException {
        
        LocalDate startDate = s;
        LocalDate endDate = e;
        
        ZonedDateTime startZone = zoneS;
        ZonedDateTime endZone = zoneE;
        
        LocalDateTime localStart = startZone.toLocalDateTime();
        LocalDateTime localEnd = endZone.toLocalDateTime();
                
        Timestamp startStamp = Timestamp.valueOf(localStart);
        Timestamp endStamp = Timestamp.valueOf(localEnd);
        
            c = JDBC.openConnection();
            PreparedStatement st = c.prepareStatement("SELECT * FROM appointments",ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
            rs = st.executeQuery();
            
            if(rs.next() == false) {
                System.out.println("hello");
                return false;
            }
            
            else {
                
                rs.beforeFirst();
                while(rs.next()) {
                    
                    Timestamp sStamp = rs.getTimestamp(6);
                    Timestamp eStamp = rs.getTimestamp(7);   
                        
                    if((startStamp.after(sStamp) || startStamp.equals(sStamp)) && endStamp.before(eStamp)) {
                        return true;
                    }
                            
                    else if((startStamp.before(sStamp) || startStamp.equals(sStamp)) && endStamp.after(eStamp)) {
                        return true;
                    }
                            
                    else if(startStamp.before(sStamp) && endStamp.after(sStamp) && (endStamp.before(eStamp) || endStamp.equals(eStamp))) {
                        return true;
                    }
                            
                    else if(startStamp.after(sStamp) && startStamp.before(eStamp) && (endStamp.after(eStamp) || endStamp.equals(eStamp))) {
                        return true;
                    } 
                    
                    else if(startStamp.equals(sStamp) && endStamp.equals(eStamp)) {
                        return true;
                    }     
                }
                
                if(startDate.isAfter(endDate)) {
                    return true;
                }
                
                else if(startStamp.after(endStamp) || startStamp.equals(endStamp)) {
                    return true;
                }
            }
        return false;  
    } 
    
    /**
     * This method changes scenes to the appointment table.
     * 
     * @param event
     * @throws Exception 
     */
    public void appointmentTable(ActionEvent event) throws Exception {
        Stage stage = (Stage)((Button)event.getSource()).getScene().getWindow();
        Parent scene = FXMLLoader.load(getClass().getResource("/view/AppointmentForm.fxml"));
        stage.setScene(new Scene(scene));
        stage.show();
    }
    
    
    /**
     * This method generates ids for new appointments.
     * 
     * @return
     * @throws SQLException 
     */
    public int generateID() throws SQLException {
        c = JDBC.openConnection();
        
            rs = c.createStatement().executeQuery("SELECT MAX(Appointment_ID) AS rowcount FROM appointments");
            rs.next();
            
            int count = rs.getInt("rowcount");
            
            int id = count + 1;
            
            return id;
    }
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        try {
            c = JDBC.openConnection();
            
            rs = c.createStatement().executeQuery("SELECT * FROM contacts");
            
            while(rs.next()) {
                Contact newContact = new Contact(rs.getInt(1), rs.getString(2), rs.getString(3));
                contacts.add(newContact);
            }
        } catch (SQLException ex) {
            Logger.getLogger(AddAppointmentController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        contact.setItems(contacts);
            
        hours.addAll("08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22");
        minutes.addAll("00", "15", "30", "45");
            
        startHour.setItems(hours);
        endHour.setItems(hours);
            
        startMin.setItems(minutes);
        endMin.setItems(minutes);
            
        types.addAll("Standard", "Premium");
        type.setItems(types);
            
        
    }    
    
}
