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

import android.content.Intent
import android.view.View
import android.view.View.VISIBLE
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.ManageAccountsActivity
import com.owncloud.android.utils.AvatarUtils
import kotlinx.android.synthetic.main.owncloud_toolbar.*

/**
 * Base class providing toolbar registration functionality, see [.setupToolbar].
 */
abstract class ToolbarActivity : BaseActivity() {

    /**
     * Toolbar setup that must be called in implementer's [.onCreate] after [.setContentView] if they
     * want to use the toolbar.
     */
    open fun setupToolbar(
        toolbarConfig: ToolbarConfig
    ) {
        when (toolbarConfig) {
            is ToolbarConfig.ToolbarStandard -> {
                configStandardToolbar(toolbarConfig)
            }
            is ToolbarConfig.ToolbarRoot -> {
                configRootToolbar(toolbarConfig)
            }
        }
    }

    private fun configStandardToolbar(standardToolbar: ToolbarConfig.ToolbarStandard) {
        useStandardToolbar(true)

        standardToolbar.title?.let { standard_toolbar?.title = it }
        setSupportActionBar(standard_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(standardToolbar.displayHomeAsUpEnabled)
        supportActionBar?.setHomeButtonEnabled(standardToolbar.homeButtonEnabled)
        supportActionBar?.setDisplayShowTitleEnabled(standardToolbar.title != null)
    }

    private fun configRootToolbar(rootToolbar: ToolbarConfig.ToolbarRoot) {
        useStandardToolbar(false)

        val toolbarTitle = findViewById<MaterialTextView>(R.id.root_toolbar_title)
        val searchView = findViewById<SearchView>(R.id.root_toolbar_search_view)
        val avatarView = findViewById<ShapeableImageView>(R.id.root_toolbar_avatar)

        with(toolbarTitle) {
            isVisible = true
            text = rootToolbar.title
            if (rootToolbar.enableSearch) {
                setOnClickListener {
                    toolbarTitle.isVisible = false
                    searchView.isVisible = true
                    searchView.isIconified = false
                }
            }
        }

        with(searchView) {
            isVisible = false
            setOnCloseListener {
                searchView.visibility = View.GONE
                toolbarTitle.visibility = VISIBLE
                false
            }
        }

        with(avatarView) {
            AccountUtils.getCurrentOwnCloudAccount(context) ?: return@with

            AvatarUtils().loadAvatarForAccount(
                avatarView,
                AccountUtils.getCurrentOwnCloudAccount(context),
                true,
                context.resources.getDimension(R.dimen.toolbar_avatar_radius)
            )
            setOnClickListener {
                startActivity(Intent(context, ManageAccountsActivity::class.java))
            }
        }
    }

    private fun useStandardToolbar(isToolbarStandard: Boolean) {
        root_toolbar?.isVisible = !isToolbarStandard
        standard_toolbar?.isVisible = isToolbarStandard
    }
}
