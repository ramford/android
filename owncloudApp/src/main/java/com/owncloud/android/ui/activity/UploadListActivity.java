/**
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author David A. Velasco
 * @author masensio
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCUpload;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CheckCurrentCredentialsOperation;
import com.owncloud.android.presentation.ui.toolbar.ToolbarConfig;
import com.owncloud.android.ui.fragment.UploadListFragment;
import com.owncloud.android.utils.MimetypeIconUtil;
import timber.log.Timber;

import java.io.File;

/**
 * Activity listing pending, active, and completed uploads. User can delete
 * completed uploads from view. Content of this list of coming from
 * {@link UploadsStorageManager}.
 */
public class UploadListActivity extends FileActivity implements UploadListFragment.ContainerActivity {

    private static final String TAG_UPLOAD_LIST_FRAGMENT = "UPLOAD_LIST_FRAGMENT";

    private UploadMessagesReceiver mUploadMessagesReceiver;

    private LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        View rightFragmentContainer = findViewById(R.id.right_fragment_container);
        rightFragmentContainer.setVisibility(View.GONE);

        // this activity has no file really bound, it's for mulitple accounts at the same time; should no inherit
        // from FileActivity; moreover, some behaviours inherited from FileActivity should be delegated to Fragments;
        // but that's other story
        setFile(null);

        // setup toolbar
        setupToolbar(new ToolbarConfig.ToolbarRoot(getResources().getString(R.string.uploads_view_title), false));

        // setup drawer
        setupDrawer();

        // setup navigation bottom bar
        setupNavigationBottomBar(R.id.nav_uploads);

        // Add fragment with a transaction for setting a tag
        if (savedInstanceState == null) {
            createUploadListFragment();
        } // else, the Fragment Manager makes the job on configuration changes

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    private void createUploadListFragment() {
        UploadListFragment uploadList = new UploadListFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.left_fragment_container, uploadList, TAG_UPLOAD_LIST_FRAGMENT);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        Timber.v("onResume() start");
        super.onResume();

        // Listen for upload messages
        mUploadMessagesReceiver = new UploadMessagesReceiver();
        IntentFilter uploadIntentFilter = new IntentFilter();
        uploadIntentFilter.addAction(FileUploader.getUploadsAddedMessage());
        uploadIntentFilter.addAction(FileUploader.getUploadStartMessage());
        uploadIntentFilter.addAction(FileUploader.getUploadFinishMessage());
        mLocalBroadcastManager.registerReceiver(mUploadMessagesReceiver, uploadIntentFilter);

        Timber.v("onResume() end");
    }

    @Override
    protected void onPause() {
        Timber.v("onPause() start");
        if (mUploadMessagesReceiver != null) {
            mLocalBroadcastManager.unregisterReceiver(mUploadMessagesReceiver);
            mUploadMessagesReceiver = null;
        }
        super.onPause();
        Timber.v("onPause() end");
    }

    // ////////////////////////////////////////
    // UploadListFragment.ContainerActivity
    // ////////////////////////////////////////
    @Override
    public boolean onUploadItemClick(OCUpload file) {
        /// TODO is this path still active?
        File f = new File(file.getLocalPath());
        if (!f.exists()) {
            showSnackMessage(
                    getString(R.string.local_file_not_found_toast)
            );

        } else {
            openFileWithDefault(file.getLocalPath());
        }
        return true;
    }

    /**
     * Open file with app associates with its MIME type. If MIME type unknown, show list with all apps.
     */
    private void openFileWithDefault(String localPath) {
        Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
        File file = new File(localPath);
        String mimetype = MimetypeIconUtil.getBestMimeTypeByFilename(localPath);
        if ("application/octet-stream".equals(mimetype)) {
            mimetype = "*/*";
        }
        myIntent.setDataAndType(Uri.fromFile(file), mimetype);
        try {
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            showSnackMessage(
                    getString(R.string.file_list_no_app_for_file_type)
            );
            Timber.i("Could not find app for sending log history.");

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        UploadsStorageManager storageManager;
        UploadListFragment uploadListFragment =
                (UploadListFragment) getSupportFragmentManager().findFragmentByTag(TAG_UPLOAD_LIST_FRAGMENT);
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
            case R.id.action_retry_uploads:
                TransferRequester requester = new TransferRequester();
                requester.retryFailedUploads(this, null, null, false);
                break;

            case R.id.action_clear_failed_uploads:
                storageManager = new UploadsStorageManager(getContentResolver());
                storageManager.clearFailedButNotDelayedForWifiUploads();
                uploadListFragment.updateUploads();
                break;

            case R.id.action_clear_successfull_uploads:
                storageManager = new UploadsStorageManager(getContentResolver());
                storageManager.clearSuccessfulUploads();
                uploadListFragment.updateUploads();
                break;

            case R.id.action_clear_finished_uploads:
                storageManager = new UploadsStorageManager(getContentResolver());
                storageManager.clearAllFinishedButNotDelayedForWifiUploads();
                uploadListFragment.updateUploads();
                break;

            default:
                retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.upload_list_menu, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileActivity.REQUEST_CODE__UPDATE_CREDENTIALS && resultCode == RESULT_OK) {
            // Retry uploads of the updated account
            Account account = AccountUtils.getOwnCloudAccountByName(
                    this,
                    data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            );
            TransferRequester requester = new TransferRequester();
            requester.retryFailedUploads(
                    this,
                    account,
                    UploadResult.CREDENTIAL_ERROR,
                    false
            );
        }
    }

    /**
     * @param operation Operation performed.
     * @param result    Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof CheckCurrentCredentialsOperation) {
            // Do not call super in this case; more refactoring needed around onRemoteOeprationFinish :'(
            getFileOperationsHelper().setOpIdWaitingFor(Long.MAX_VALUE);
            dismissLoadingDialog();
            Account account = ((RemoteOperationResult<Account>) result).getData();
            if (!result.isSuccess()) {

                requestCredentialsUpdate();

            } else {
                // already updated -> just retry!
                TransferRequester requester = new TransferRequester();
                requester.retryFailedUploads(this, account, UploadResult.CREDENTIAL_ERROR, false);
            }

        } else {
            super.onRemoteOperationFinish(operation, result);
        }
    }

    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new UploadListServiceConnection();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private class UploadListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (service instanceof FileUploaderBinder) {
                if (mUploaderBinder == null) {
                    mUploaderBinder = (FileUploaderBinder) service;
                    Timber.d("UploadListActivity connected to Upload service. component: " + component + " service: " + service);
                    // Say to UploadListFragment that the Binder is READY in the Activity
                    UploadListFragment uploadListFragment =
                            (UploadListFragment) getSupportFragmentManager().findFragmentByTag(TAG_UPLOAD_LIST_FRAGMENT);
                    if (uploadListFragment != null) {
                        uploadListFragment.binderReady();
                    }
                } else {
                    Timber.d("mUploaderBinder already set. mUploaderBinder: " +
                            mUploaderBinder + " service:" + service);
                }
            } else {
                Timber.d("UploadListActivity not connected to Upload service. component: " + component + " service: " + service);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(UploadListActivity.this, FileUploader.class))) {
                Timber.d("UploadListActivity suddenly disconnected from Upload service");
                mUploaderBinder = null;
            }
        }
    }

    /**
     * Once the file upload has changed its status -> update uploads list view
     */
    private class UploadMessagesReceiver extends BroadcastReceiver {
        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            UploadListFragment uploadListFragment =
                    (UploadListFragment) getSupportFragmentManager().findFragmentByTag(TAG_UPLOAD_LIST_FRAGMENT);

            uploadListFragment.updateUploads();
        }
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (mAccountWasSet) {
            setAccountInDrawer(getAccount());
        }
    }

}
