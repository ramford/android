/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.ui.toolbar

import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.textview.MaterialTextView
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.BaseActivity
import kotlinx.android.synthetic.main.owncloud_toolbar.*

/**
 * Base class providing toolbar registration functionality, see [.setupToolbar].
 */
abstract class ToolbarActivity : BaseActivity() {

    private var currentToolbarStatus: ToolbarStatus = ToolbarStatus.TOOLBAR_STANDARD

    /**
     * Toolbar setup that must be called in implementer's [.onCreate] after [.setContentView] if they
     * want to use the toolbar.
     */
    protected fun setupToolbar(
        toolbarStatus: ToolbarStatus
    ) {
        currentToolbarStatus = toolbarStatus
        when (toolbarStatus) {
            ToolbarStatus.TOOLBAR_PICKER -> {
                root_toolbar.isVisible = false
                standard_toolbar.isVisible = true
                setSupportActionBar(standard_toolbar)
            }
            ToolbarStatus.TOOLBAR_STANDARD -> {
                root_toolbar.isVisible = false
                standard_toolbar.isVisible = true
                setSupportActionBar(standard_toolbar)
            }
            ToolbarStatus.TOOLBAR_ROOT -> {
                root_toolbar.isVisible = true
                standard_toolbar.isVisible = false
            }
        }
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected open fun updateActionBarTitleAndHomeButton(chosenFile: OCFile?) {
        var title = ""

        if (isRoot(chosenFile)) {
            currentToolbarStatus = ToolbarStatus.TOOLBAR_ROOT
            title = getString(R.string.actionbar_search_in)
        } else {
            currentToolbarStatus = ToolbarStatus.TOOLBAR_STANDARD
            setupToolbar(currentToolbarStatus)
            standard_toolbar.navigationIcon = ContextCompat.getDrawable(baseContext, R.drawable.ic_arrow_back)
        }
        setupToolbar(currentToolbarStatus)
        // choose the appropriate title
        title = if (!isRoot(chosenFile)) {
            title + " " + chosenFile?.fileName
        } else {
            title + " " + getString(R.string.default_display_name_for_root_folder)
        }
        updateActionBarTitleAndHomeButtonByString(title)
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected fun updateActionBarTitleAndHomeButtonByString(title: String?) {
        var titleToSet: String? = getString(R.string.app_name) // default
        if (title != null) {
            titleToSet = title
        }

        // set the chosen title

        if (currentToolbarStatus == ToolbarStatus.TOOLBAR_ROOT) {
            val toolbarTitle = findViewById<MaterialTextView>(R.id.root_toolbar_title)
            val searchview = findViewById<SearchView>(R.id.root_toolbar_search_view)
            toolbarTitle.text = titleToSet
            toolbarTitle.visibility = View.VISIBLE
            searchview.visibility = View.GONE
            toolbarTitle.setOnClickListener { v: View? ->
                searchview.visibility = View.VISIBLE
                toolbarTitle.visibility = View.GONE
                searchview.isIconified = false
            }
            searchview.setOnCloseListener {
                searchview.visibility = View.GONE
                toolbarTitle.visibility = View.VISIBLE
                false
            }
        } else {
            standard_toolbar.title = titleToSet
        }
    }

    /**
     * checks if the given file is the root folder.
     *
     * @param file file to be checked if it is the root folder
     * @return `true` if it is `null` or the root folder, else returns `false`
     */
    fun isRoot(file: OCFile?): Boolean {
        return file == null || file.isFolder && file.parentId == FileDataStorageManager.ROOT_PARENT_ID.toLong()
    }
}
