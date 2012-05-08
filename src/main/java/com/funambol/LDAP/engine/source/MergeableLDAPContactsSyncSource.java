package com.funambol.LDAP.engine.source;

import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.engine.source.MergeableSyncSource;
import com.funambol.framework.engine.source.SyncSourceException;

public class MergeableLDAPContactsSyncSource extends
		ExtendedLDAPContactsSyncSource  implements MergeableSyncSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1351151620585100963L;

	/**
	 * merge two sync item and eventually updates client with info stored on server
	 */
	public boolean mergeSyncItems(SyncItemKey syncItemKey, SyncItem syncItem)
			throws SyncSourceException {
        try {
            boolean clientUpdateRequired = false;
            
            // merge items
            
            // eventually update item on server
//                l.mergeItems(syncItemKey.getKeyAsString(),
//                                   convert(syncItem)           ,
//                                   syncItem.getTimestamp()
//                );

            if(clientUpdateRequired) {
                syncItem = getSyncItemFromId(syncItemKey);
            }
            return clientUpdateRequired;

        } catch(Exception e) {
            logger.error("SyncSource error: a merge did not succeed.", e);
            throw new SyncSourceException("Error merging SyncItem with key '"
                                        + syncItemKey
                                        + "' with SyncItem '"
                                        + syncItem
                                        + "'", e);
        }

	}

}
