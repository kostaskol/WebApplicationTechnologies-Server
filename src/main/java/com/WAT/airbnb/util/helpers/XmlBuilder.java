package com.WAT.airbnb.util.helpers;

import com.jamesmurty.utils.XMLBuilder;

import javax.xml.parsers.ParserConfigurationException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;

import static java.lang.String.valueOf;

/**
 *  Uses James Murty's XMLBuilder class (https://github.com/jmurty/java-xmlbuilder)
 *  Accepts 5 ResultSet objects that correspond to the 5 main tables in the Data Base
 *  and returns an XMLBuilder object which can be used to either generate a java.lang.String object
 *  or a file
 *  @author Kostas Kolivas
 *  @version 1.0
 */
public class XmlBuilder {
    static public XMLBuilder getXml(ResultSet users, ResultSet houses, ResultSet comments, ResultSet bookings,
                                    ResultSet messages) {
        try {
            XMLBuilder xmlBuilder = XMLBuilder.create("Airbnb")
                    .e("Users");

            while (users.next()) {
                if (users.getString("email").equals("root")) continue;
                xmlBuilder = xmlBuilder.e("User")
                        .e("UserID")
                        .t(valueOf(users.getInt("userID")))
                        .up()
                        .e("Email")
                        .t(users.getString("email"))
                        .up();
                    String accType = users.getString("accType");
                    if (accType == null) {
                        accType = "N/A";
                    }
                xmlBuilder = xmlBuilder.e("AccountType")
                        .t(accType)
                        .up();
                String fName = users.getString("firstName");
                if (fName == null) {
                    fName = "N/A";
                }
                xmlBuilder = xmlBuilder.e("FirstName")
                        .t(users.getString("firstName"))
                        .up()
                        .e("LastName")
                        .t(users.getString("lastName"))
                        .up()
                        .e("PhoneNumber")
                        .t(users.getString("phoneNumber") == null ? "N/A" : users.getString("phoneNumber"))
                        .up()
                        .e("DateOfBirth");
                        String dob;
                        if (users.getDate("dateOfBirth") == null) {
                            dob = "N/A";
                        } else {
                            dob = DateHelper.dateToString(users.getDate("dateOfBirth"));
                        }
                        xmlBuilder = xmlBuilder
                        .t(dob)
                        .up();
                        String country;
                        if (users.getString("country") == null) {
                            country = "N/A";
                        } else {
                            country = users.getString("country");
                        }
                        xmlBuilder = xmlBuilder
                        .e("Country")
                        .t(country)
                        .up();
                String bio = users.getString("bio");
                if (bio == null) {
                    bio = "Unavailable";
                }
                xmlBuilder = xmlBuilder
                        .e("Bio")
                        .t(bio)
                        .up()
                        .e("Approved")
                        .t(valueOf(users.getBoolean("approved")))
                        .up()
                        .up();
            }

            xmlBuilder = xmlBuilder.up();

            xmlBuilder = xmlBuilder.e("Houses");

            while(houses.next()) {
                xmlBuilder = xmlBuilder.e("House")
                        .e("HouseID")
                        .t(valueOf(houses.getInt("houseID")))
                        .up()
                        .e("OwnerID")
                        .t(valueOf(houses.getInt("ownerID")))
                        .up()
                        .e("Latitude")
                        .t(valueOf(houses.getFloat("latitude")))
                        .up()
                        .e("Longitude")
                        .t(valueOf(houses.getFloat("longitude")))
                        .up();

                String address = houses.getString("address");
                if (address == null) {
                    address = "Unavailable";
                }

                xmlBuilder = xmlBuilder
                        .e("Address")
                        .t(address)
                        .up();

                String city = houses.getString("city");
                if (city == null) {
                    city = "Unavailable";
                }

                xmlBuilder = xmlBuilder
                        .e("City")
                        .t(city)
                        .up();

                String country = houses.getString("country");
                if (country == null) {
                    country = "Unavailable";
                }

                xmlBuilder = xmlBuilder
                        .e("Country")
                        .t(country)
                        .up()
                        .e("NumberOfBeds")
                        .t(valueOf(houses.getInt("numBeds")))
                        .up()
                        .e("NumberOfBaths")
                        .t(valueOf(houses.getInt("numBaths")))
                        .up()
                        .e("Accommodates")
                        .t(valueOf(houses.getInt("accommodates")))
                        .up()
                        .e("LivingRoom")
                        .t(valueOf(houses.getBoolean("hasLivingRoom")))
                        .up()
                        .e("SmokingAllowed")
                        .t(valueOf(houses.getBoolean("smokingAllowed")))
                        .up()
                        .e("PetsAllowed")
                        .t(valueOf(houses.getBoolean("petsAllowed")))
                        .up()
                        .e("EventsAllowed")
                        .t(valueOf(houses.getBoolean("eventsAllowed")))
                        .up()
                        .e("WiFi")
                        .t(valueOf(houses.getBoolean("wifi")))
                        .up()
                        .e("Airconditioning")
                        .t(valueOf(houses.getBoolean("airconditioning")))
                        .up()
                        .e("Heating")
                        .t(valueOf(houses.getBoolean("heating")))
                        .up()
                        .e("Kitchen")
                        .t(valueOf(houses.getBoolean("kitchen")))
                        .up()
                        .e("TV")
                        .t(valueOf(houses.getBoolean("tv")))
                        .up()
                        .e("Parking")
                        .t(valueOf(houses.getBoolean("parking")))
                        .up()
                        .e("Elevator")
                        .t(valueOf(houses.getBoolean("elevator")))
                        .up()
                        .e("Area")
                        .t(valueOf(houses.getFloat("area")))
                        .up()
                        .e("Description")
                        .t(houses.getString("description"))
                        .up()
                        .e("Instructions")
                        .t(houses.getString("instructions"))
                        .up()
                        .e("MinimumDays")
                        .t(valueOf(houses.getInt("minDays")))
                        .up()
                        .e("NumberOfRatings")
                        .t(valueOf(houses.getInt("numRatings")))
                        .up()
                        .e("DateFrom")
                        .t(DateHelper.dateToString(houses.getDate("dateFrom")))
                        .up()
                        .e("DateTo")
                        .t(DateHelper.dateToString(houses.getDate("dateTo")))
                        .up()
                        .e("MinimumCost")
                        .t(valueOf(houses.getFloat("minCost")))
                        .up()
                        .e("CostPerPerson")
                        .t(valueOf(houses.getFloat("costPerPerson")))
                        .up()
                        .up();
            }

            xmlBuilder = xmlBuilder.up();

            // There are currently 691,055 entries in the comments table
            // (~350MB)
            // If used in debugging, please comment out the following section

            // SECTION START
            xmlBuilder = xmlBuilder.e("Comments");

            while (comments.next()) {
                xmlBuilder = xmlBuilder.e("Comment")
                        .e("CommentID")
                        .t(valueOf(comments.getInt("commentID")))
                        .up()
                        .e("UserID")
                        .t(valueOf(comments.getInt("userID")))
                        .up()
                        .e("HouseID")
                        .t(valueOf(comments.getInt("houseID")))
                        .up()
                        .e("Comment")
                        .t(comments.getString("comm"))
                        .up()
                        .e("Rating")
                        .t(valueOf(comments.getFloat("rating")))
                        .up()
                        .up();
            }

            xmlBuilder = xmlBuilder.up();
            // SECTION END

            xmlBuilder = xmlBuilder.e("Messages");

            while (messages.next()) {
                xmlBuilder = xmlBuilder.e("Message")
                        .e("MessageID")
                        .t(valueOf(messages.getInt("messageID")))
                        .up()
                        .e("SenderID")
                        .t(valueOf(messages.getInt("senderID")))
                        .up()
                        .e("ReceiverID")
                        .t(valueOf(messages.getInt("receiverID")))
                        .up()
                        .e("Message")
                        .t(messages.getString("message"))
                        .up()
                        .e("Deleted")
                        .t(messages.getString("deleted"))
                        .up()
                        .up();
            }

            xmlBuilder = xmlBuilder.up();

            xmlBuilder = xmlBuilder.e("Bookings");

            while (bookings.next()) {
                xmlBuilder = xmlBuilder.e("Booking")
                        .e("BookingID")
                        .t(valueOf(bookings.getInt("bookingID")))
                        .up()
                        .e("UserID")
                        .t(valueOf(bookings.getInt("userID")))
                        .up()
                        .e("HouseID")
                        .t(valueOf(bookings.getInt("houseID")))
                        .up()
                        .e("NumberOfGuests")
                        .t(valueOf(bookings.getInt("guests")))
                        .up()
                        .e("DateFrom")
                        .t(DateHelper.dateToString(bookings.getDate("dateFrom")))
                        .up()
                        .e("DateTo")
                        .t(DateHelper.dateToString(bookings.getDate("dateTo")))
                        .up()
                        .up();
            }

            xmlBuilder = xmlBuilder.up();

            return xmlBuilder;
        } catch (ParserConfigurationException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
