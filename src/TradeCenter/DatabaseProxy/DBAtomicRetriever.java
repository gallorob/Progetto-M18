package TradeCenter.DatabaseProxy;

import TradeCenter.Card.Card;
import TradeCenter.Card.Description;
import TradeCenter.Card.PokemonDescription;
import TradeCenter.Card.YuGiOhDescription;
import TradeCenter.Customers.Customer;
import TradeCenter.Trades.Trade;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

/**
 * Single records retriever (database management module)
 * @author Roberto Gallotta
 */
class DBAtomicRetriever {

    /**
     * Retrieve a single customer from the database given its username (used in LogIn procedure)
     * @param connection: database connection
     * @param username: customer's username
     * @return customer
     */
    Customer retrieveSingleCustomerByUsername(Connection connection, String username) {
        Customer customer = null;
        try {
            System.err.println("[DBAtomicRetriever] - Retrieving customer by username " + username + "...\n");
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM customers where username = ?;");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                customer = new Customer(rs.getString("customer_id"),
                        rs.getString("username"),
                        rs.getString("password"));
            }
            System.err.println("[DBAtomicRetriever] - Customer with username " + username + " retrieved.\n");
        }catch (SQLException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method retrieveSingleCustomerByUsername.\n");
        }
        return customer;
    }

    /**
     * Retrieve a single customer from the database given its id (used in trades procedures)
     * @param connection: database connection
     * @param id: customer's id
     * @return customer
     */
    Customer retrieveSingleCustomerByUserID(Connection connection, int id) {
        Customer customer = null;
        try {
            System.err.println("[DBAtomicRetriever] - Retrieving customer by id " + id + "...\n");
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM customers where customer_id = ?;");
            String user_id = "USER-"+id;
            ps.setString(1, user_id);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                customer = new Customer(rs.getString("customer_id"),
                        rs.getString("username"),
                        rs.getString("password"));
            }
            System.err.println("[DBAtomicRetriever] - Customer with id " + id + " retrieved.\n");
        }catch (SQLException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method retrieveSingleCustomerByUserID.\n");
        }
        return customer;
    }

    /**
     * Get the size of the selected table
     * @param connection: database connection
     * @param tablename: table name
     * @return table size
     */
    int getTableSize(Connection connection, String tablename) {
        int size = 0;
        try {
            PreparedStatement ps = null;
            switch (tablename) {
                case "customers":
                    ps = connection.prepareStatement("SELECT COUNT(*) FROM customers;");
                    break;
                case "cards":
                    ps = connection.prepareStatement("SELECT COUNT(*) FROM cards;");
                    break;
                case "trades":
                    ps = connection.prepareStatement("SELECT COUNT(*) FROM trades;");
                    break;
                case "pokemon_card":
                    ps = connection.prepareStatement("SELECT COUNT(*) FROM pokemon_card;");
                    break;
                case "yugioh_card":
                    ps = connection.prepareStatement("SELECT COUNT(*) FROM yugioh_card;");
                    break;
            }
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                size = rs.getInt(1);
                return size;
            }
        }catch (SQLException | NullPointerException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method getTableSize for table " + tablename + ".\n");
        }
        return size;
    }

    /**
     * Retrieve a single Pokémon description given its id
     * @param connection: database connection
     * @param id: Pokémon description id
     * @return Pokémon description
     */
    PokemonDescription retrieveSinglePokemonDescription(Connection connection, int id) {
        PokemonDescription pokemonDescription = null;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM pokemon_card WHERE pokemon_description_id = ?;");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                Blob blob = rs.getBlob("Picture");
                byte[] bytes = blob.getBytes(1, (int)blob.length());
                BufferedImage picture = ImageIO.read(new ByteArrayInputStream(bytes));

                String text = rs.getString("Description");
                if(rs.wasNull()) {
                    text = "NO DESCRIPTION AVAILABLE";
                } //could be NULL
                int hp = rs.getInt("Hp");
                if(rs.wasNull()) {
                    hp = 0;
                } //could be NULL
                int weight = rs.getInt("Weigth");
                if(rs.wasNull()) {
                    weight = 0;
                } //could be NULL
                String length = rs.getString("Length");
                if(rs.wasNull()) {
                    length = "";
                } //could be NULL
                int level = rs.getInt("Level");
                if(rs.wasNull()) {
                    level = 0;
                } //could be NULL

                pokemonDescription = new PokemonDescription(rs.getString("Name"),
                        text, picture,
                        rs.getInt("pokemon_description_id"),
                        rs.getString("Type"),
                        hp,
                        weight,
                        length,
                        level);
            }
        } catch (SQLException | IOException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method retrieveSinglePokemonDescription for description " + id + ".\n");
        }
        return pokemonDescription;
    }

    /**
     * Retrieve a single Yu-Gi-Oh description given its id
     * @param connection: database connection
     * @param id: Yu-Gi-Oh description id
     * @return Yu-Gi-Oh description
     */
    YuGiOhDescription retrieveSingleYugiohDescription(Connection connection, int id) {
        YuGiOhDescription yuGiOhDescription = null;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM yugioh_card AS a LEFT JOIN (SELECT Monster_Type_ID, Name AS MonsterTypeName FROM monster_Type) AS b ON a.Monster_Type_ID = b.Monster_Type_ID LEFT JOIN (SELECT Type_ID, Name AS CardTypeName FROM card_Type) AS c ON a.Type_ID = c.Type_ID WHERE yugioh_description_id = ?;");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                Blob blob = rs.getBlob("Picture");
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                BufferedImage picture = ImageIO.read(new ByteArrayInputStream(bytes));

                int level = rs.getInt("Level");
                if(rs.wasNull()) {
                    level = 0;
                }
                int atk = rs.getInt("Atk");
                if(rs.wasNull()) {
                    atk = 0;
                }
                int def = rs.getInt("Def");
                if(rs.wasNull()) {
                    def = 0;
                }
                String monsterTypeName = rs.getString("MonsterTypeName");
                if(rs.wasNull()) {
                    monsterTypeName = "NOT A MONSTER";
                }

                yuGiOhDescription = new YuGiOhDescription(
                        rs.getString("Name"),
                        rs.getString("Description"),
                        picture,
                        rs.getInt("yugioh_description_id"),
                        rs.getString("Reference"),
                        level,
                        atk,
                        def,
                        monsterTypeName,
                        rs.getString("CardTypeName")
                );
            }
        } catch (SQLException | IOException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method retrieveSingleYugiohDescription for description " + id + ".\n");
        }
        return yuGiOhDescription;
    }

    /**
     * Retrieve the cards in the selected customer's collection
     * @param connection: database connection
     * @param customer: selected customer //TODO change this to customer ID for less visibility?
     * @return array of cards
     */
    ArrayList<Card> retrieveCardsInCustomerCollection(Connection connection, Customer customer) {
        ArrayList<Card> cards = new ArrayList<>();
        try {
            System.err.println("[DBAtomicRetriever] - Retrieving customer " + customer.getId() + "'s collection...\n");
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM cards WHERE customer_id = ?;");
            ps.setString(1, customer.getId());
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Description description = null;
                switch (rs.getString("card_Type")) {
                    case "pokemon":
                        description = retrieveSinglePokemonDescription(connection, rs.getInt("description_id"));
                        break;
                    case "yugioh":
                        description = retrieveSingleYugiohDescription(connection, rs.getInt("description_id"));
                        break;
                }
                cards.add(new Card(rs.getInt("card_id"), description));
            }
            System.err.println("[DBAtomicRetriever] - Retrieved customer " + customer.getId() + "'s collection.\n");
        } catch (SQLException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method retrieveCardsInCustomerCollection.\n");
        }
        return cards;
    }

    ArrayList<Card> retrieveCardsInTradeOffer(Connection connection, int trade_id, int offer_col) {
        ArrayList<Card> cards = new ArrayList<>();
        try {
            System.err.println("[DBAtomicRetriever] - Retrieving cards in trade " + trade_id + "'s offer " + offer_col + "...\n");
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM cards WHERE trade_id = ? AND offer_col= ?");
            ps.setInt(1, trade_id);
            ps.setInt(2, offer_col);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Description description = null;
                switch (rs.getString("card_Type")) {
                    case "pokemon":
                        description = retrieveSinglePokemonDescription(connection, rs.getInt("description_id"));
                        break;
                    case "yugioh":
                        description = retrieveSingleYugiohDescription(connection, rs.getInt("description_id"));
                        break;
                }
                cards.add(new Card(rs.getInt("card_id"), description));
            }
            System.err.println("[DBAtomicRetriever] - Ccards in trade " + trade_id + "'s offer " + offer_col + " retrieved.\n");
        } catch (SQLException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method retrieveCardsInTradeOffer.\n");
        }
        return cards;
    }

    /**
     * Retrieve the descriptions in the selected customer's wishlist
     * @param connection: database connection
     * @param customer: selected customer //TODO change this to customer ID for less visibility?
     * @return array of descriptions
     */
    ArrayList<Description> retrieveDescriptionsInCustomerWishlist(Connection connection, Customer customer) {
        ArrayList<Description> wishlist = new ArrayList<>();
        try {
            System.err.println("[DBAtomicRetriever] - Retrieving customer " + customer.getId() + "'wishlist...\n");
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM wishlist WHERE customer_id = ?");
            ps.setString(1, customer.getId());
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Description description = null;
                switch (rs.getString("type")) {
                    case "pokemon":
                        description = retrieveSinglePokemonDescription(connection, rs.getInt("description_id"));
                        break;
                    case "yugioh":
                        description = retrieveSingleYugiohDescription(connection, rs.getInt("description_id"));
                        break;
                }
                wishlist.add(description);
            }
            System.err.println("[DBAtomicRetriever] - Customer " + customer.getId() + "'wishlist retrieved.\n");
        } catch (SQLException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method retrieveDescriptionsInCustomerWishlist.\n");
        }
        return wishlist;
    }

    /**
     * Retrieve a trade given its id
     * @param connection: database connection
     * @param id: trade id
     * @return trade
     */
    Trade retrieveTrade(Connection connection, int id) {
        Trade trade = null;
        try {
            System.err.println("[DBAtomicRetriever] - Retrieving trade number " + id + "...\n");
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM trades WHERE trade_id = ?;");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            FakeOffer offer = new FakeOffer();
            while(rs.next()) {
                offer.setId(rs.getInt("trade_id"));
                offer.setCustomer1(rs.getString("user1_id"));
                offer.setCustomer2(rs.getString("user2_id"));
                offer.setDate(rs.getDate("date"));
                Boolean donedeal = rs.getBoolean("donedeal");
                offer.setDoneDeal(donedeal);
                offer.setPositiveEnd(rs.getBoolean("positive_end"));
                PreparedStatement ps1;
                PreparedStatement ps2;
                Card card;
                if (donedeal) {
                    //first collection
                    ps1 = connection.prepareStatement("SELECT * FROM cards_old WHERE trade_id = ? AND offer_col = ?;");
                    ps1.setInt(1, id);
                    ps1.setInt(2, 1);
                    //second collection
                    ps2 = connection.prepareStatement("SELECT * FROM cards_old WHERE trade_id = ? AND offer_col = ?;");
                    ps2.setInt(1, id);
                    ps2.setInt(2, 2);
                } else {
                    //first collection
                    ps1 = connection.prepareStatement("SELECT * FROM cards WHERE trade_id = ? AND offer_col = ?;");
                    ps1.setInt(1, id);
                    ps1.setInt(2, 1);
                    //second collection
                    ps2 = connection.prepareStatement("SELECT * FROM cards WHERE trade_id = ? AND offer_col = ?;");
                    ps2.setInt(1, id);
                    ps2.setInt(2, 2);
                }
                ResultSet rs1 = ps1.executeQuery();
                while (rs1.next()) {
                    Description description = null;
                    switch (rs1.getString("card_Type")) {
                        case "pokemon":
                            description = retrieveSinglePokemonDescription(connection, rs1.getInt("description_id"));
                            break;
                        case "yugioh":
                            description = retrieveSingleYugiohDescription(connection, rs1.getInt("description_id"));
                            break;
                    }
                    card = new Card(rs1.getInt("card_id"), description);
                    offer.addCardOffer1(card);
                }
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    Description description = null;
                    switch (rs2.getString("card_Type")) {
                        case "pokemon":
                            description = retrieveSinglePokemonDescription(connection, rs2.getInt("description_id"));
                            break;
                        case "yugioh":
                            description = retrieveSingleYugiohDescription(connection, rs2.getInt("description_id"));
                            break;
                    }
                    card = new Card(rs2.getInt("card_id"), description);
                    offer.addCardOffer2(card);
                }
            }
            trade = new Trade(offer);
            System.err.println("[DBAtomicRetriever] - Trade number " + id + " retrieved.\n");
        } catch (SQLException e) {
            System.err.println("[DBAtomicRetriever] - Exception " + e + " encounterd in method retrieveTrade.\n");
        }
        return trade;
    }

}