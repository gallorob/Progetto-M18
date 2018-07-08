package TradeCenter.DatabaseProxy;

import Interface.searchCard.filterChoice.PokemonAll;
import Interface.searchCard.filterChoice.YuGiOhAll;
import TradeCenter.Card.*;
import TradeCenter.Customers.Customer;
import TradeCenter.Trades.Trade;

import java.sql.Array;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Proxy for database management
 * @author Roberto Gallotta
 */
public class DBProxy implements IProxy,ISearch{

    private Connection connection;
    private DBAtomicRetriever dbRet = new DBAtomicRetriever();
    private DBAtomicInserter dbIns = new DBAtomicInserter();
    private DBAtomicUpdater dbUp = new DBAtomicUpdater();
    private DBAtomicDeleter dbDel = new DBAtomicDeleter();
    private DBConnectionManager dbConn = new DBConnectionManager();
    //Singleton
    private DBSearchDescription dbSearch=DBSearchDescription.getInstance();

    /**
     * Found Pokemon Descriptions which match with the filter
     * @param pokFilter: object pokemonAll
     * @return Descriptions foundend matched
     */
    @Override
    public HashSet<PokemonDescription> getFoundDescrPokemon(PokemonAll pokFilter) {
        String typeInput = pokFilter.getType();
        String len1 = pokFilter.getLen1();
        String len2 = pokFilter.getLen2();
        int hpInput = pokFilter.getHp();
        int lev = pokFilter.getLev();
        int weigth = pokFilter.getWeigth();

        connection = dbConn.connectToDB(connection);

        HashSet<PokemonDescription> descrFound;
        descrFound = dbSearch.getSearchedDescrPokemon(connection, typeInput, hpInput, lev, weigth, len1, len2);
        connection = dbConn.disconnectFromDB(connection);
        return descrFound;
    }

    /**
     * Found YuGiOh Descriptions which match with the filter
     * @param yugiohFilter: To filter     *
     * @return YuGiOhDescription matched
     */
    @Override
    public HashSet<YuGiOhDescription> getFoundDescrYugioh(YuGiOhAll yugiohFilter) {
        String reference=yugiohFilter.getReference();
        int lev=yugiohFilter.getLev();
        int atk=yugiohFilter.getAtk();
        int def=yugiohFilter.getDef();
        String monsterID=yugiohFilter.getMonsterType();
        String typeID=yugiohFilter.getType();

        connection = dbConn.connectToDB(connection);
        HashSet<YuGiOhDescription> descrFound;
        descrFound = dbSearch.getSearchedDescrYuGiOh(connection,reference,lev,atk,def,monsterID,typeID);
        connection = dbConn.disconnectFromDB(connection);
        return descrFound;
    }
    /**
     * Populates catalog with chosen descriptions
     * @param cc: catalog to populate
     * @param tablename: type of descriptions to load
     */
    @Override
    public void populateCatalog(CardCatalog cc, String tablename) {
        connection = dbConn.connectToDB(connection);
        int size;
        switch(tablename) {
            case "pokemon_card":
                size = dbRet.getTableSize(connection, tablename);
                for(int i = 0; i<size; i++) {
                    cc.addDescription(dbRet.retrieveSinglePokemonDescription(connection, i));
                }
                break;
            case "yugioh_card":
                size = dbRet.getTableSize(connection, tablename);
                for(int i = 0; i<size; i++) {
                    cc.addDescription(dbRet.retrieveSingleYugiohDescription(connection, i));
                }
                break;
        }
        connection = dbConn.disconnectFromDB(connection);

    }

    /**
     * Adds full customer to customer hashmap
     * @param customers: hashmap to be populated
     */
    @Override
    public void retrieveCustomers(HashMap<String, Customer> customers) {
        connection = dbConn.connectToDB(connection);
        int n = dbRet.getTableSize(connection, "customers");
        for(int i=1; i<= n; i++) {
            Customer customer = dbRet.retrieveSingleCustomerByUserID(connection, i);
            // add card collection
            for(Card card: dbRet.retrieveCardsInCustomerCollection(connection, customer)) {
                customer.addCard(card);
            }
            // add card wishlist
            for(Description description: dbRet.retrieveDescriptionsInCustomerWishlist(connection, customer)) {
                customer.addCardToWishList(description);
            }
            // add customer
            customers.put(customer.getId(), customer);
        }
        connection = dbConn.disconnectFromDB(connection);
    }

    /**
     * Retrieve a single customer from the database given its username (used in LogIn procedure)
     * @param username: customer's username
     * @return customer
     */
    @Override
    public Customer retrieveSingleCustomer(String username) {
        connection = dbConn.connectToDB(connection);
        Customer customer = null;
        customer = dbRet.retrieveSingleCustomerByUsername(connection, username);
        connection = dbConn.disconnectFromDB(connection);
        return customer;
    }

    /**
     * Adds a customer in the database
     * @param customer: customer to be added
     */
    @Override
    public void addCustomerToDatabase(Customer customer) {
        connection = dbConn.connectToDB(connection);
        // add customer
        dbIns.insertCustomer(connection, customer);
        // add customer's wishlist
        for (Description description : customer.getWishList()) {
            dbIns.insertWishlist(connection, description, customer);
        }
        // add customer's collection
        for (Card card : customer.getCollection()) {
            dbIns.insertCard(connection, card, customer);
        }
        connection = dbConn.disconnectFromDB(connection);
    }

    /**
     * Updates a customer (customer's collection and wishlist)
     * @param customer: customer to be updated
     */
    @Override
    public void updateCustomer(Customer customer) {
        connection = dbConn.connectToDB(connection);
        // update customer's cards
            //get db's customer's collection
        ArrayList<Card> oldCollection = dbRet.retrieveCardsInCustomerCollection(connection, customer);
            //get cards to update
        ArrayList<Card> toUpdate = new ArrayList<Card>(customer.getCollection().getSet());
        toUpdate.removeAll(oldCollection);
        for(Card card:toUpdate) {
            dbUp.updateCard(connection, card, customer.getId());
        }
            //free memory
        oldCollection.clear();
        toUpdate.clear();
        // update customer's wishlist
            //get db's wishlist
        ArrayList<Description> oldWishlist = dbRet.retrieveDescriptionsInCustomerWishlist(connection, customer);
            //get wishlists to add
        ArrayList<Description> toAdd = new ArrayList<Description>(customer.getWishList());
        toAdd.removeAll(oldWishlist);
            //update db
        for(Description description:toAdd) {
            dbIns.insertWishlist(connection, description, customer);
        }
            //get wishlists to remove
        oldWishlist.removeAll(customer.getWishList());
            //update db
        for(Description description:oldWishlist) {
            dbDel.removeWishlist(connection, customer, description);
        }
            //free memory
        toAdd.clear();
        oldWishlist.clear();
        connection = dbConn.disconnectFromDB(connection);
    }

    /**
     * Get a trade given its id
     * @param id: trade id
     * @return: trade
     */
    @Override
    public Trade getTrade(int id) {
        connection = dbConn.connectToDB(connection);
        Trade trade = dbRet.retrieveTrade(connection, id);
        connection = dbConn.disconnectFromDB(connection);
        return trade;
    }

    @Override
    public void updateTrade(Trade trade) {
        connection = dbConn.connectToDB(connection);
        // update trade's data
        dbUp.updateTrade(connection, trade);
        if(trade.isDoneDeal()) {
            // update cards in offer1
                // get db's offer1
            ArrayList<Card> oldOffer1 = dbRet.retrieveCardsInTradeOffer(connection, trade.getId(), 1);
                // get cards to add
            ArrayList<Card> toAdd = new ArrayList<Card>(trade.getOffer1().getSet());
            toAdd.removeAll(oldOffer1);
            for(Card card : toAdd) {
                dbUp.updateCard(connection, card, trade.getId(), 1);
            }
                // get cards to remove
            ArrayList<Card> toRemove = new ArrayList<Card>(trade.getOffer1().getSet());
            oldOffer1.removeAll(toRemove);
            for(Card card : oldOffer1) {
                dbUp.updateCard(connection, card);
            }
            // update cards in offer2
            // get db's offer2
            ArrayList<Card> oldOffer2 = dbRet.retrieveCardsInTradeOffer(connection, trade.getId(), 2);
            // get cards to add
            ArrayList<Card> toAdd2 = new ArrayList<Card>(trade.getOffer1().getSet());
            toAdd.removeAll(oldOffer2);
            for(Card card : toAdd) {
                dbUp.updateCard(connection, card, trade.getId(), 2);
            }
            // get cards to remove
            ArrayList<Card> toRemove2 = new ArrayList<Card>(trade.getOffer1().getSet());
            oldOffer1.removeAll(toRemove2);
            for(Card card : oldOffer2) {
                dbUp.updateCard(connection, card);
            }
            // free memory
            oldOffer1.clear();
            toAdd.clear();
            toRemove.clear();
            toAdd2.clear();
            toRemove2.clear();
        } else {
            // update cards in offer1
                // get db's offer1
            ArrayList<Card> oldOffer1 = dbRet.retrieveCardsInTradeOffer(connection, trade.getId(), 1);
                // get new cards
            ArrayList<Card> newCards = new ArrayList<Card>(trade.getOffer1().getSet());
            newCards.removeAll(oldOffer1);
                // set to null all these cards
            for (Card card: oldOffer1) {
                dbUp.updateCard(connection, card);
            }
            for (Card card : newCards) {
                dbUp.updateCard(connection, card);
            }
                // save new cards to db (old_cards)
            for (Card card : trade.getOffer1()) {
                dbIns.insertTradedCard(connection, card, trade.getCustomer1(), trade.getId(), 1);
            }
            // get db's offer2
            ArrayList<Card> oldOffer2 = dbRet.retrieveCardsInTradeOffer(connection, trade.getId(), 2);
            // get new cards
            ArrayList<Card> newCards2 = new ArrayList<Card>(trade.getOffer2().getSet());
            newCards.removeAll(oldOffer2);
            // set to null all these cards
            for (Card card: oldOffer2) {
                dbUp.updateCard(connection, card);
            }
            for (Card card : newCards2) {
                dbUp.updateCard(connection, card);
            }
            // save new cards to db (old_cards)
            for (Card card : trade.getOffer2()) {
                dbIns.insertTradedCard(connection, card, trade.getCustomer2(), trade.getId(), 2);
            }
            // free memory
            oldOffer1.clear();
            oldOffer2.clear();
            newCards.clear();
            newCards2.clear();
        }
        connection = dbConn.disconnectFromDB(connection);
    }

    /**
     * Insert a new trade in the database
     * @param trade: trade to be added
     */
    @Override
    public void InsertTrade(Trade trade) {
        connection = dbConn.connectToDB(connection);
        // insert trade data
        dbIns.insertTrade(connection, trade);
        // update cards in the offers
        for (Card card : trade.getOffer1()) {
            dbUp.updateCard(connection, card, trade.getId(), 1);
        }
        for (Card card : trade.getOffer2()) {
            dbUp.updateCard(connection, card, trade.getId(), 2);
        }
    }

    /**
     * Quick method to get next card ID
     * @return: next card ID
     */
    @Override
    public int getNextCardID() {
        connection = dbConn.connectToDB(connection);
        int n = dbRet.getTableSize(connection, "cards") + 1;
        connection = dbConn.disconnectFromDB(connection);
        return n;
    }

    /**
     * Quick method to get next customer ID
     * @return: next customer ID
     */
    @Override
    public int getNextCustomerID() {
        connection = dbConn.connectToDB(connection);
        int n = dbRet.getTableSize(connection, "customers") + 1;
        connection = dbConn.disconnectFromDB(connection);
        return n;
    }

    @Override
    public int getNextTradeID() {
        connection = dbConn.connectToDB(connection);
        int n = dbRet.getTableSize(connection, "trades") + 1;
        connection = dbConn.disconnectFromDB(connection);
        return n;
    }
}
