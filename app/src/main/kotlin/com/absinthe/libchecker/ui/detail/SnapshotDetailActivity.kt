package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.bean.SnapshotDetailItem
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.databinding.ActivitySnapshotDetailBinding
import com.absinthe.libchecker.recyclerview.VerticalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotDetailAdapter
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.BaseSnapshotNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotNativeNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotTitleNode
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.snapshot.SnapshotDetailDeletedView
import com.absinthe.libchecker.view.snapshot.SnapshotDetailNewInstallView
import com.absinthe.libchecker.view.snapshot.SnapshotEmptyView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import rikka.insets.setInitialPadding

const val EXTRA_ENTITY = "EXTRA_ENTITY"

class SnapshotDetailActivity : BaseActivity<ActivitySnapshotDetailBinding>() {

  private lateinit var entity: SnapshotDiffItem

  private val adapter by unsafeLazy { SnapshotDetailAdapter(lifecycleScope) }
  private val viewModel: SnapshotViewModel by viewModels()
  private val _entity by unsafeLazy { intent.getSerializableExtra(EXTRA_ENTITY) as? SnapshotDiffItem }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (_entity != null) {
      entity = _entity!!
      initView()
      viewModel.computeDiffDetail(this, entity)
    } else {
      onBackPressed()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(item)
  }

  @SuppressLint("SetTextI18n")
  private fun initView() {
    setSupportActionBar(binding.toolbar)
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
      title = entity.labelDiff.new ?: entity.labelDiff.old
    }

    binding.apply {
      list.apply {
        adapter = this@SnapshotDetailActivity.adapter
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        addItemDecoration(VerticalSpacesItemDecoration(4.dp))
      }

      val isNewOrDeleted = entity.deleted || entity.newInstalled

      ivAppIcon.apply {
        val appIconLoader = AppIconLoader(
          resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
          false,
          this@SnapshotDetailActivity
        )
        val icon = try {
          appIconLoader.loadIcon(
            PackageUtils.getPackageInfo(
              entity.packageName,
              PackageManager.GET_META_DATA
            ).applicationInfo
          )
        } catch (e: PackageManager.NameNotFoundException) {
          null
        }
        load(icon)
        setOnClickListener {
          lifecycleScope.launch {
            val lcItem = Repositories.lcRepository.getItem(entity.packageName) ?: return@launch
            LCAppUtils.launchDetailPage(this@SnapshotDetailActivity, lcItem)
          }
        }
      }
      tvAppName.text = getDiffString(entity.labelDiff, isNewOrDeleted)
      tvPackageName.text = entity.packageName
      tvVersion.text = getDiffString(
        entity.versionNameDiff,
        entity.versionCodeDiff,
        isNewOrDeleted,
        "%s (%s)"
      )
      tvTargetApi.text = "API ${getDiffString(entity.targetApiDiff, isNewOrDeleted)}"
    }

    viewModel.snapshotDetailItems.observe(this) { details ->
      val titleList = mutableListOf<SnapshotTitleNode>()

      getNodeList(details.filter { it.itemType == NATIVE }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, NATIVE))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Native", this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == SERVICE }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, SERVICE))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Service", this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == ACTIVITY }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, ACTIVITY))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Activity", this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == RECEIVER }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, RECEIVER))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Receiver", this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == PROVIDER }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, PROVIDER))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Provider", this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == PERMISSION }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, PERMISSION))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Permission", this.size.toLong())
          )
        }
      }

      if (titleList.isNotEmpty()) {
        adapter.setList(titleList)
      }
    }

    adapter.setEmptyView(
      when {
        entity.newInstalled -> SnapshotDetailNewInstallView(this)
        entity.deleted -> SnapshotDetailDeletedView(this)
        else -> SnapshotEmptyView(this).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
          ).also {
            it.gravity = Gravity.CENTER_HORIZONTAL
          }
          addPaddingTop(96.dp)
        }
      }
    )
    adapter.setOnItemClickListener { _, view, position ->
      if (adapter.data[position] is SnapshotTitleNode) {
        adapter.expandOrCollapse(position)
        return@setOnItemClickListener
      }
      if (AntiShakeUtils.isInvalidClick(view)) {
        return@setOnItemClickListener
      }

      val item = (adapter.data[position] as BaseSnapshotNode).item
      if (item.diffType == REMOVED || item.itemType == PERMISSION) {
        return@setOnItemClickListener
      }

      lifecycleScope.launch {
        val lcItem = Repositories.lcRepository.getItem(entity.packageName) ?: return@launch
        LCAppUtils.launchDetailPage(
          this@SnapshotDetailActivity,
          item = lcItem,
          refName = item.name,
          refType = item.itemType
        )
      }
    }

    lifecycleScope.launchWhenStarted {
      if (!LCAppUtils.atLeastR()) {
        binding.list.also {
          it.post {
            it.setInitialPadding(
              0,
              0,
              0,
              window.decorView.rootWindowInsets?.systemWindowInsetBottom
                ?: 0
            )
          }
        }
      }
    }
  }

  private fun getNodeList(list: List<SnapshotDetailItem>): MutableList<BaseNode> {
    val returnList = mutableListOf<BaseNode>()

    if (list.isEmpty()) return returnList

    if (list[0].itemType == NATIVE) {
      list.forEach { returnList.add(SnapshotNativeNode(it)) }
    } else {
      list.forEach { returnList.add(SnapshotComponentNode(it)) }
    }

    return returnList
  }

  private fun <T> getDiffString(
    diff: SnapshotDiffItem.DiffNode<T>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s"
  ): String {
    return if (diff.old != diff.new && !isNewOrDeleted) {
      "${format.format(diff.old)} $ARROW ${format.format(diff.new)}"
    } else {
      format.format(diff.old)
    }
  }

  private fun getDiffString(
    diff1: SnapshotDiffItem.DiffNode<*>,
    diff2: SnapshotDiffItem.DiffNode<*>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s"
  ): String {
    return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
      "${format.format(diff1.old, diff2.old)} $ARROW ${format.format(diff1.new, diff2.new)}"
    } else {
      format.format(diff1.old, diff2.old)
    }
  }
}
