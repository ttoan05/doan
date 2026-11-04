package com.example.foodhub.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.SearchHistoryManager
import com.example.foodhub.data.VoucherStore
import com.example.foodhub.ui.cart.CartActivity
import com.example.foodhub.ui.voucher.VoucherActivity
import com.example.foodhub.utils.SampleData

/**
 * Tách riêng toàn bộ phần render và logic của Tab Trang chủ (Home Content)
 * Bao gồm Header, Giỏ hàng, Search bar, Gợi ý tìm kiếm, Banner khuyến mãi, Danh mục,
 * Bộ lọc & Sắp xếp đa tiêu chí nâng cao, và Danh sách món ăn / nhà hàng.
 */
object MainActivityHomeView {

    fun showHomeContent(activity: MainActivity) {
        val container = activity.contentContainer
        container.removeAllViews()

        // ── 1. Header (Tiêu đề + Giỏ hàng) ───────────────────────────────────
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.dp(16), activity.dp(16), activity.dp(16), activity.dp(12))
        }
        val tvTitle = TextView(activity).apply {
            text = "FoodHub"
            textSize = 20f
            setTextColor(activity.getColor(R.color.red_primary))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val cartContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(activity.dp(55), activity.dp(55))
        }

        val tvCart = TextView(activity).apply {
            text = "🛒"
            textSize = 25f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(activity.dp(55), activity.dp(55))
            setOnClickListener {
                activity.startActivity(Intent(activity, CartActivity::class.java))
            }
        }

        val cartBadgeView = TextView(activity).apply {
            text = CartManager.totalQuantity().toString()
            textSize = 10f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E5394F"))
            }
            layoutParams = FrameLayout.LayoutParams(activity.dp(22), activity.dp(22)).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = activity.dp(2)
                rightMargin = activity.dp(0)
            }
            visibility = if (CartManager.totalQuantity() > 0) View.VISIBLE else View.GONE
        }
        activity.cartBadge = cartBadgeView

        cartContainer.addView(tvCart)
        cartContainer.addView(cartBadgeView)
        header.addView(tvTitle)
        header.addView(cartContainer)
        container.addView(header)

        // ── 2. Dữ liệu nguồn & Trạng thái bộ lọc ─────────────────────────────
        val allFoods = DatabaseHelper.getInstance(activity).getAllFoods().ifEmpty { SampleData.FOODS }
        val allRestaurants = DatabaseHelper.getInstance(activity).getAllRestaurants().ifEmpty { SampleData.RESTAURANTS }
        val filterState = HomeFilterState()

        var foodAdapter: FoodAdapter? = null
        var restaurantAdapter: RestaurantAdapter? = null

        // Thông báo tóm tắt bộ lọc
        val tvFilterSummary = TextView(activity).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#E63946"))
            setTypeface(null, Typeface.BOLD)
            setPadding(activity.dp(16), 0, activity.dp(16), activity.dp(6))
            visibility = View.GONE
        }

        val tvSearchEmpty = TextView(activity).apply {
            text = "Không tìm thấy món ăn hoặc nhà hàng phù hợp với bộ lọc"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(activity.getColor(R.color.text_grey))
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(activity.dp(16), activity.dp(24), activity.dp(16), activity.dp(16))
            layoutParams = lp
        }

        var updateAllFilterChipsUi: (() -> Unit)? = null
        var updateCatButtonsUi: (() -> Unit)? = null

        fun applyAllFilters() {
            val filteredFoods = HomeFilterSortHelper.filterAndSortFoods(allFoods, allRestaurants, filterState)
            val filteredRestaurants = HomeFilterSortHelper.filterAndSortRestaurants(allRestaurants, filterState)

            foodAdapter?.submit(filteredFoods)
            restaurantAdapter?.submit(filteredRestaurants)

            val summary = filterState.getActiveFilterSummary()
            if (summary.isNotEmpty()) {
                tvFilterSummary.text = "🎯 Đang lọc: $summary (${filteredFoods.size} món)"
                tvFilterSummary.visibility = View.VISIBLE
            } else {
                tvFilterSummary.visibility = View.GONE
            }

            val noResult = filterState.isFiltered() && filteredFoods.isEmpty() && filteredRestaurants.isEmpty()
            tvSearchEmpty.visibility = if (noResult) View.VISIBLE else View.GONE
        }

        // ── 3. Ô tìm kiếm ───────────────────────────────────────────────────
        val searchLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_search)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(activity.dp(16), activity.dp(12), activity.dp(16), activity.dp(8))
            layoutParams = lp
            setPadding(activity.dp(14), activity.dp(4), activity.dp(10), activity.dp(4))
        }

        val etSearch = EditText(activity).apply {
            hint = "Tìm kiếm món ăn, nhà hàng..."
            textSize = 14f
            setTextColor(activity.getColor(R.color.text_dark))
            setHintTextColor(activity.getColor(R.color.text_grey))
            setBackgroundColor(Color.TRANSPARENT)
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
            setPadding(0, activity.dp(8), 0, activity.dp(8))
        }

        val btnClearSearch = ImageView(activity).apply {
            setImageResource(R.drawable.ic_search_clear)
            val lp = LinearLayout.LayoutParams(activity.dp(24), activity.dp(24))
            layoutParams = lp
            setPadding(activity.dp(3), activity.dp(3), activity.dp(3), activity.dp(3))
            visibility = View.GONE
            background = null
        }

        searchLayout.addView(etSearch)
        searchLayout.addView(btnClearSearch)
        container.addView(searchLayout)

        // ── 4. Suggestion View (Lịch sử tìm kiếm & Từ khóa Hot) ──────────────
        val suggestionView = activity.layoutInflater.inflate(R.layout.layout_search_suggestions, null)
        val llRecentHeader = suggestionView.findViewById<LinearLayout>(R.id.llRecentHeader)
        val hsvRecent = suggestionView.findViewById<HorizontalScrollView>(R.id.hsvRecentSearches)
        val llRecentTags = suggestionView.findViewById<LinearLayout>(R.id.llRecentTags)
        val btnClearAllHistory = suggestionView.findViewById<TextView>(R.id.btnClearHistory)
        val llHotTags = suggestionView.findViewById<LinearLayout>(R.id.llHotTags)
        val llFilterChips = suggestionView.findViewById<LinearLayout>(R.id.llFilterChips)

        fun refreshRecentTags() {
            val recents = SearchHistoryManager.getRecentSearches(activity)
            if (recents.isEmpty()) {
                llRecentHeader.visibility = View.GONE
                hsvRecent.visibility = View.GONE
            } else {
                llRecentHeader.visibility = View.VISIBLE
                hsvRecent.visibility = View.VISIBLE
                llRecentTags.removeAllViews()
                recents.forEach { query ->
                    val tagView = activity.layoutInflater.inflate(R.layout.item_recent_search_tag, llRecentTags, false)
                    val tvRecentTitle = tagView.findViewById<TextView>(R.id.tvTagTitle)
                    val btnRemove = tagView.findViewById<ImageView>(R.id.btnRemoveTag)
                    tvRecentTitle.text = query
                    tagView.setOnClickListener {
                        etSearch.setText(query)
                        etSearch.setSelection(query.length)
                        SearchHistoryManager.addSearchQuery(activity, query)
                        refreshRecentTags()
                        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                    }
                    btnRemove.setOnClickListener {
                        SearchHistoryManager.removeSearchQuery(activity, query)
                        refreshRecentTags()
                    }
                    llRecentTags.addView(tagView)
                }
            }
        }

        btnClearAllHistory.setOnClickListener {
            SearchHistoryManager.clearAll(activity)
            refreshRecentTags()
        }

        val hotKeywords = listOf("Trà sữa", "Cơm sườn", "Bánh mì chảo", "Freeship", "Gà rán", "Phở")
        llHotTags.removeAllViews()
        hotKeywords.forEach { hotKey ->
            val tv = TextView(activity).apply {
                text = hotKey
                textSize = 12f
                setTextColor(Color.parseColor("#BE123C"))
                setBackgroundResource(R.drawable.bg_search_tag_hot)
                setPadding(activity.dp(12), activity.dp(6), activity.dp(12), activity.dp(6))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, activity.dp(8), 0)
                layoutParams = lp
                setOnClickListener {
                    etSearch.setText(hotKey)
                    etSearch.setSelection(hotKey.length)
                    SearchHistoryManager.addSearchQuery(activity, hotKey)
                    refreshRecentTags()
                    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                }
            }
            llHotTags.addView(tv)
        }

        // Tích hợp các chip tìm kiếm gợi ý đồng bộ cùng filterState
        data class SuggestChip(val id: String, val label: String)
        val suggestChips = listOf(
            SuggestChip("under_35k", "Dưới 35k"),
            SuggestChip("rating_45", "Đánh giá 4.5+ ⭐"),
            SuggestChip("freeship", "🚚 Freeship"),
            SuggestChip("fast", "⚡ Giao nhanh"),
            SuggestChip("best_seller", "🔥 Bán chạy")
        )
        val suggestChipViews = mutableMapOf<String, TextView>()

        fun updateSuggestChipsUi() {
            suggestChips.forEach { chip ->
                val tv = suggestChipViews[chip.id] ?: return@forEach
                val isSelected = when (chip.id) {
                    "under_35k" -> filterState.filterUnder35k
                    "rating_45" -> filterState.filterRating45
                    "freeship" -> filterState.filterFreeship
                    "fast" -> filterState.filterFastDelivery
                    "best_seller" -> filterState.sortOption == HomeSortOption.BEST_SELLER
                    else -> false
                }
                if (isSelected) {
                    tv.setBackgroundResource(R.drawable.bg_filter_chip_active)
                    tv.setTextColor(Color.WHITE)
                    tv.setTypeface(null, Typeface.BOLD)
                } else {
                    tv.setBackgroundResource(R.drawable.bg_filter_chip_normal)
                    tv.setTextColor(Color.parseColor("#475569"))
                    tv.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        llFilterChips.removeAllViews()
        suggestChips.forEach { chip ->
            val tv = TextView(activity).apply {
                text = chip.label
                textSize = 12f
                setPadding(activity.dp(12), activity.dp(6), activity.dp(12), activity.dp(6))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, activity.dp(8), 0)
                layoutParams = lp
                setOnClickListener {
                    when (chip.id) {
                        "under_35k" -> filterState.filterUnder35k = !filterState.filterUnder35k
                        "rating_45" -> filterState.filterRating45 = !filterState.filterRating45
                        "freeship" -> filterState.filterFreeship = !filterState.filterFreeship
                        "fast" -> filterState.filterFastDelivery = !filterState.filterFastDelivery
                        "best_seller" -> filterState.sortOption = if (filterState.sortOption == HomeSortOption.BEST_SELLER) HomeSortOption.POPULAR else HomeSortOption.BEST_SELLER
                    }
                    updateSuggestChipsUi()
                    updateAllFilterChipsUi?.invoke()
                    applyAllFilters()
                }
            }
            suggestChipViews[chip.id] = tv
            llFilterChips.addView(tv)
        }
        updateSuggestChipsUi()
        refreshRecentTags()

        suggestionView.visibility = View.GONE
        container.addView(suggestionView)

        fun onSearchQueryChanged(text: String) {
            filterState.searchQuery = text
            btnClearSearch.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
            applyAllFilters()
        }

        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || etSearch.text.isNotEmpty()) {
                refreshRecentTags()
                suggestionView.visibility = View.VISIBLE
            }
        }

        etSearch.setOnClickListener {
            refreshRecentTags()
            suggestionView.visibility = View.VISIBLE
        }

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            filterState.searchQuery = ""
            applyAllFilters()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onSearchQueryChanged(s?.toString().orEmpty())
            }
        })

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q = etSearch.text.toString().trim()
                if (q.isNotEmpty()) {
                    SearchHistoryManager.addSearchQuery(activity, q)
                    refreshRecentTags()
                }
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                true
            } else {
                false
            }
        }

        // ── 5. Promo Banner ──────────────────────────────────────────────────
        if (!VoucherStore.isWelcomeBannerDismissed(activity)) {
            val promoBanner = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_promo)
                gravity = Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(activity.dp(16), 0, activity.dp(16), activity.dp(16))
                layoutParams = lp
                setPadding(activity.dp(14), activity.dp(12), activity.dp(14), activity.dp(12))
            }
            val promoText = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val promoTitle = TextView(activity).apply {
                text = "🎉 Giảm 30% cho đơn đầu tiên"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(activity.getColor(R.color.red_primary))
            }
            val promoSub = TextView(activity).apply {
                text = "Nhập mã: FIRST30"
                textSize = 12f
                setTextColor(activity.getColor(R.color.red_primary))
            }
            promoText.addView(promoTitle)
            promoText.addView(promoSub)
            val promoBtn = Button(activity).apply {
                text = "DÙNG NGAY"
                textSize = 13f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setBackgroundResource(R.drawable.bg_btn_red)
                setOnClickListener {
                    VoucherStore.markWelcomeBannerDismissed(activity)
                    try {
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Voucher Code", "FIRST30")
                        clipboard.setPrimaryClip(clip)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    activity.showToast("Đã nhận mã FIRST30! Mở Ví Voucher của bạn")
                    promoBanner.visibility = View.GONE
                    container.removeView(promoBanner)
                    activity.startActivity(Intent(activity, VoucherActivity::class.java))
                }
            }
            promoBanner.addView(promoText)
            promoBanner.addView(promoBtn)
            container.addView(promoBanner)
        }

        // ── 6. Categories (Danh mục ẩm thực) ─────────────────────────────────
        val catScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, activity.dp(12))
            layoutParams = lp
        }
        val catRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(activity.dp(16), 0, activity.dp(16), 0)
        }
        val catButtons = mutableListOf<Button>()

        updateCatButtonsUi = {
            catButtons.forEachIndexed { i, btn ->
                val catName = SampleData.CATEGORIES.getOrNull(i) ?: ""
                val isActive = catName.equals(filterState.category, ignoreCase = true)
                btn.setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                btn.setTextColor(if (isActive) Color.WHITE else activity.getColor(R.color.text_grey2))
                btn.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
            }
        }

        SampleData.CATEGORIES.forEach { cat ->
            val isActive = cat.equals(filterState.category, ignoreCase = true)
            val btn = Button(activity).apply {
                text = cat
                textSize = 14f
                setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                setTextColor(if (isActive) Color.WHITE else activity.getColor(R.color.text_grey2))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, activity.dp(8), 0)
                layoutParams = lp
                setOnClickListener {
                    filterState.category = cat
                    updateCatButtonsUi?.invoke()
                    applyAllFilters()
                }
            }
            catButtons.add(btn)
            catRow.addView(btn)
        }
        catScroll.addView(catRow)
        container.addView(catScroll)

        // ── 7. BỘ LỌC & SẮP XẾP NÂNG CAO (Advanced Filter & Sort Bar) ───────────
        val filterSortScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, activity.dp(12))
            layoutParams = lp
        }
        val filterSortRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.dp(16), 0, activity.dp(16), 0)
        }

        // Nút Đặt lại bộ lọc (chỉ hiện khi có lọc/sắp xếp khác mặc định)
        val btnReset = TextView(activity).apply {
            text = "↺ Đặt lại"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(activity.getColor(R.color.red_primary))
            setBackgroundResource(R.drawable.bg_cat_normal)
            setPadding(activity.dp(12), activity.dp(7), activity.dp(12), activity.dp(7))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, activity.dp(8), 0)
            layoutParams = lp
            setOnClickListener {
                filterState.reset()
                etSearch.setText("")
                updateAllFilterChipsUi?.invoke()
                updateSuggestChipsUi()
                updateCatButtonsUi?.invoke()
                applyAllFilters()
                activity.showToast("Đã đặt lại bộ lọc")
            }
        }
        filterSortRow.addView(btnReset)

        data class HomeChipDef(
            val id: String,
            val label: String,
            val isSort: Boolean,
            val sortVal: HomeSortOption? = null
        )

        val homeChips = listOf(
            HomeChipDef("bestseller", "🔥 Bán chạy", true, HomeSortOption.BEST_SELLER),
            HomeChipDef("rating", "⭐ Đánh giá cao", true, HomeSortOption.RATING),
            HomeChipDef("price_asc", "Giá: Thấp → Cao", true, HomeSortOption.PRICE_ASC),
            HomeChipDef("price_desc", "Giá: Cao → Thấp", true, HomeSortOption.PRICE_DESC),
            HomeChipDef("nearby", "📍 Gần nhất", true, HomeSortOption.NEARBY),
            HomeChipDef("freeship", "🚚 Freeship", false),
            HomeChipDef("under35k", "💰 Dưới 35k", false),
            HomeChipDef("rating45", "⭐ 4.5+ sao", false),
            HomeChipDef("fast", "⚡ Giao < 25p", false)
        )

        val homeChipViews = mutableMapOf<String, TextView>()

        updateAllFilterChipsUi = {
            btnReset.visibility = if (filterState.isFiltered()) View.VISIBLE else View.GONE
            homeChips.forEach { chipDef ->
                val tv = homeChipViews[chipDef.id] ?: return@forEach
                val isSelected = if (chipDef.isSort) {
                    filterState.sortOption == chipDef.sortVal
                } else {
                    when (chipDef.id) {
                        "freeship" -> filterState.filterFreeship
                        "under35k" -> filterState.filterUnder35k
                        "rating45" -> filterState.filterRating45
                        "fast" -> filterState.filterFastDelivery
                        else -> false
                    }
                }
                if (isSelected) {
                    tv.setBackgroundResource(R.drawable.bg_filter_chip_active)
                    tv.setTextColor(Color.WHITE)
                    tv.setTypeface(null, Typeface.BOLD)
                } else {
                    tv.setBackgroundResource(R.drawable.bg_filter_chip_normal)
                    tv.setTextColor(Color.parseColor("#475569"))
                    tv.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        homeChips.forEach { chipDef ->
            val tv = TextView(activity).apply {
                text = chipDef.label
                textSize = 12f
                setPadding(activity.dp(12), activity.dp(7), activity.dp(12), activity.dp(7))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, activity.dp(8), 0)
                layoutParams = lp
                setOnClickListener {
                    if (chipDef.isSort) {
                        filterState.sortOption = if (filterState.sortOption == chipDef.sortVal) {
                            HomeSortOption.POPULAR
                        } else {
                            chipDef.sortVal ?: HomeSortOption.POPULAR
                        }
                    } else {
                        when (chipDef.id) {
                            "freeship" -> filterState.filterFreeship = !filterState.filterFreeship
                            "under35k" -> filterState.filterUnder35k = !filterState.filterUnder35k
                            "rating45" -> filterState.filterRating45 = !filterState.filterRating45
                            "fast" -> filterState.filterFastDelivery = !filterState.filterFastDelivery
                        }
                    }
                    updateAllFilterChipsUi?.invoke()
                    updateSuggestChipsUi()
                    applyAllFilters()
                }
            }
            homeChipViews[chipDef.id] = tv
            filterSortRow.addView(tv)
        }
        filterSortScroll.addView(filterSortRow)
        container.addView(filterSortScroll)
        container.addView(tvFilterSummary)

        // ── 8. Section: Món ngon đề xuất ─────────────────────────────────────
        activity.addSectionHeader("Món ngon đề xuất", "Xem tất cả") { activity.showToast("Xem tất cả món") }
        val foodRecycler = RecyclerView(activity).apply {
            layoutManager = GridLayoutManager(activity, 2)
            adapter = FoodAdapter(
                initialFoods = allFoods,
                onItemClick = { food -> activity.openFoodDetail(food) },
                onFavoriteChanged = { food, isFav, oldTime -> activity.onFoodHeartChanged(food, isFav, oldTime) },
                onAddToCart = { food -> activity.onAddToCart(food) }
            ).also { foodAdapter = it; activity.homeFoodAdapter = it }

            isNestedScrollingEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(activity.dp(10), 0, activity.dp(10), activity.dp(24))
            layoutParams = lp
        }
        container.addView(foodRecycler)

        // ── 9. Section: Nhà hàng nổi bật ─────────────────────────────────────
        activity.addSectionHeader("Nhà hàng nổi bật", "Xem thêm") { activity.showToast("Xem thêm nhà hàng") }
        val restaurantRecycler = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = RestaurantAdapter(
                initialRestaurants = allRestaurants,
                onFavoriteChanged = { r, isFav, oldTime -> activity.onRestaurantHeartChanged(r, isFav, oldTime) },
                onViewMenu = { r -> activity.openRestaurantDetail(r) }
            ).also { restaurantAdapter = it; activity.homeRestaurantAdapter = it }
            isNestedScrollingEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(activity.dp(16), 0, activity.dp(16), activity.dp(16))
            layoutParams = lp
        }
        container.addView(restaurantRecycler)
        container.addView(tvSearchEmpty)

        // Kích hoạt hiển thị ban đầu
        updateAllFilterChipsUi.invoke()
        applyAllFilters()
    }
}
