/*
 * This file is generated by jOOQ.
*/
package generated_resources.tables.records;


import generated_resources.tables.AdminUsers;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Row1;
import org.jooq.impl.TableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.6"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AdminUsersRecord extends TableRecordImpl<AdminUsersRecord> implements Record1<String> {

    private static final long serialVersionUID = 1706827293;

    /**
     * Setter for <code>test.admin_users.uId</code>.
     */
    public void setUid(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>test.admin_users.uId</code>.
     */
    public String getUid() {
        return (String) get(0);
    }

    // -------------------------------------------------------------------------
    // Record1 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row1<String> fieldsRow() {
        return (Row1) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row1<String> valuesRow() {
        return (Row1) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field1() {
        return AdminUsers.ADMIN_USERS.UID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component1() {
        return getUid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value1() {
        return getUid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdminUsersRecord value1(String value) {
        setUid(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdminUsersRecord values(String value1) {
        value1(value1);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AdminUsersRecord
     */
    public AdminUsersRecord() {
        super(AdminUsers.ADMIN_USERS);
    }

    /**
     * Create a detached, initialised AdminUsersRecord
     */
    public AdminUsersRecord(String uid) {
        super(AdminUsers.ADMIN_USERS);

        set(0, uid);
    }
}