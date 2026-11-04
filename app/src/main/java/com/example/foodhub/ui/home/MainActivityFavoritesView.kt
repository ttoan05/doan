package com.example.foodhub.ui.home

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.model.FavSort
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.SampleData

/**
 * Tách riêng toàn bộ phần render và logic của Tab Yêu thích (Favorites Content).
 */
class MainActivityFavoritesView(private val activity: MainActivity) {

    private var favTab = "foods"
    private var favSearch = ""
    private var favCategory = "Tất cả"
    private var favSort = FavSort.NEWEST

    private val foodSorts = listOf(FavSort.NEWEST, FavSort.PRICE_ASC, FavSort.PRICE_DESC, FavSort.RATING)
    private val restaurantSorts = listOf(FavSort.NEWEST, FavSort.RATING, FavSort.NAME)

    var onRefreshRequested: (() -> Unit)? = null

    private fun sortFavFoods(list: List<Food>): List<Food> = when (favSort) {
        FavSort.PRICE_ASC -> list.sortedBy { FormatUtils.parsePrice(it.price) }
        FavSort.PRICE_DESC -> list.sortedByDescending { FormatUtils.parsePrice(it.price) }
        FavSort.RATING -> list.sortedByDescending { it.rating }
        else -> list.sortedByDescending { FavoriteManager.foodAddedAt(it.id) }
    }

    private fun sortFavRestaurants(list: List<Restaurant>): List<Restaurant> = when (favSort) {
        FavSort.RATING -> list.sortedByDescending { it.rating }
        FavSort.NAME -> list.sortedBy { it.name }
        else -> list.sortedByDescending { FavoriteManager.restaurantAddedAt(it.id) }
    }

    private fun suggestFoods(favFoods: List<Food>): List<Food> {
        val notFav = SampleData.FOODS.filter { !FavoriteManager.isFood(it.id) }
        val favCategories = favFoods.map { it.category }.toSet()
        val sameCategory = notFav.filter { it.category in favCategories }
        val others = notFav.filter { it !in sameCategory }.sortedByDescending { it.rating }
        return (sameCategory + others).take(4)
    }

    fun showFavoritesContent() {
        val container = activity.contentContainer
        container.removeAllViews()
        activity.cartBadge = null
        activity.layoutInflater.inflate(R.layout.layout_favorites, container, true)

        val tvSummary = activity.findViewById<TextView>(R.id.tvFavSummary)
        val btnClear = activity.findViewById<TextView>(R.id.btnFavClear)
        val etSearch = activity.findViewById<EditText>(R.id.etFavSearch)
        val tabFoods = activity.findViewById<View>(R.id.tabFavFoods)
        val tabRestaurants = activity.findViewById<View>(R.id.tabFavRestaurants)
        val tvTabFoods = activity.findViewById<TextView>(R.id.tvTabFavFoods)
        val tvTabRestaurants = activity.findViewById<TextView>(R.id.tvTabFavRestaurants)
        val vTabFoods = activity.findViewById<View>(R.id.vTabFavFoods)
        val vTabRestaurants = activity.findViewById<View>(R.id.vTabFavRestaurants)
        val scrollCats = activity.findViewById<View>(R.id.scrollFavCats)
        val llCats = activity.findViewById<LinearLayout>(R.id.llFavCats)
        val llToolbar = activity.findViewById<View>(R.id.llFavToolbar)
        val btnSort = activity.findViewById<TextView>(R.id.btnFavSort)
        val btnAddAll = activity.findViewById<TextView>(R.id.btnFavAddAll)
        val rvFoods = activity.findViewById<RecyclerView>(R.id.rvFavFoods)
        val rvRestaurants = activity.findViewById<RecyclerView>(R.id.rvFavRestaurants)
        val tvNoResult = activity.findViewById<View>(R.id.tvFavNoResult)
        val llEmpty = activity.findViewById<View>(R.id.llFavEmpty)
        val tvEmptyTitle = activity.findViewById<TextView>(R.id.tvFavEmptyTitle)
        val tvEmptyDesc = activity.findViewById<TextView>(R.id.tvFavEmptyDesc)
        val btnExplore = activity.findViewById<Button>(R.id.btnFavExplore)
        val tvSuggestTitle = activity.findViewById<TextView>(R.id.tvFavSuggestTitle)
        val rvSuggest = activity.findViewById<RecyclerView>(R.id.rvFavSuggest)

        val red = activity.getColor(R.color.red_primary)
        val grey = activity.getColor(R.color.text_grey)

        val foodAdapter = FoodAdapter(
            initialFoods = emptyList(),
            onItemClick = { food -> activity.openFoodDetail(food) },
            onFavoriteChanged = { food, isFav, oldTime -> activity.onFoodHeartChanged(food, isFav, oldTime) },
            onAddToCart = { food -> activity.onAddToCart(food) }
        )
        val suggestAdapter = FoodAdapter(
            initialFoods = emptyList(),
            onItemClick = { food -> activity.openFoodDetail(food) },
            onFavoriteChanged = { food, isFav, oldTime -> activity.onFoodHeartChanged(food, isFav, oldTime) },
            onAddToCart = { food -> activity.onAddToCart(food) }
        )
        val restaurantAdapter = RestaurantAdapter(
            initialRestaurants = emptyList(),
            onFavoriteChanged = { r, isFav, oldTime -> activity.onRestaurantHeartChanged(r, isFav, oldTime) },
            onViewMenu = { r -> activity.openRestaurantDetail(r) }
        )
        rvFoods.layoutManager = GridLayoutManager(activity, 2)
        rvFoods.adapter = foodAdapter
        rvSuggest.layoutManager = GridLayoutManager(activity, 2)
        rvSuggest.adapter = suggestAdapter
        rvRestaurants.layoutManager = LinearLayoutManager(activity)
        rvRestaurants.adapter = restaurantAdapter

        val catButtons = mutableListOf<Button>()
        var shownFoods: List<Food> = emptyList()

        fun refresh() {
            val isFoods = favTab == "foods"
            val q = favSearch.trim().lowercase()

            val sorts = if (isFoods) foodSorts else restaurantSorts
            if (favSort !in sorts) favSort = FavSort.NEWEST

            val allFoods = FavoriteManager.favoriteFoods()
            val allRestaurants = FavoriteManager.favoriteRestaurants()

            var foods = allFoods
            if (favCategory != "Tất cả") foods = foods.filter { it.category.trim().equals(favCategory.trim(), ignoreCase = true) }
            if (q.isNotEmpty()) {
                foods = foods.filter {
                    com.example.foodhub.utils.FormatUtils.matchesSearch(it.name, q) ||
                    com.example.foodhub.utils.FormatUtils.matchesSearch(it.restaurant, q)
                }
            }
            foods = sortFavFoods(foods)
            shownFoods = foods

            var restaurants = allRestaurants
            if (q.isNotEmpty()) {
                restaurants = restaurants.filter {
                    com.example.foodhub.utils.FormatUtils.matchesSearch(it.name, q) ||
                    com.example.foodhub.utils.FormatUtils.matchesSearch(it.desc, q)
                }
            }
            restaurants = sortFavRestaurants(restaurants)

            foodAdapter.submit(foods)
            restaurantAdapter.submit(restaurants)

            val total = if (isFoods) allFoods.size else allRestaurants.size
            val shown = if (isFoods) foods.size else restaurants.size

            tvSummary.text = "${allFoods.size} món • ${allRestaurants.size} nhà hàng"
            tvTabFoods.text = "Món ăn (${allFoods.size})"
            tvTabRestaurants.text = "Nhà hàng (${allRestaurants.size})"
            tvTabFoods.setTextColor(if (isFoods) red else grey)
            tvTabRestaurants.setTextColor(if (isFoods) grey else red)
            tvTabFoods.setTypeface(null, if (isFoods) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            tvTabRestaurants.setTypeface(null, if (isFoods) android.graphics.Typeface.NORMAL else android.graphics.Typeface.BOLD)
            vTabFoods.setBackgroundColor(if (isFoods) red else Color.TRANSPARENT)
            vTabRestaurants.setBackgroundColor(if (isFoods) Color.TRANSPARENT else red)

            etSearch.visibility = if (allFoods.isEmpty() && allRestaurants.isEmpty()) View.GONE else View.VISIBLE
            btnClear.visibility = if (total > 0) View.VISIBLE else View.GONE
            scrollCats.visibility = if (isFoods && allFoods.isNotEmpty()) View.VISIBLE else View.GONE
            llToolbar.visibility = if (total > 0) View.VISIBLE else View.GONE
            btnAddAll.visibility = if (isFoods) View.VISIBLE else View.GONE
            btnSort.text = "⇅  ${favSort.label}"
            rvFoods.visibility = if (isFoods && foods.isNotEmpty()) View.VISIBLE else View.GONE
            rvRestaurants.visibility = if (!isFoods && restaurants.isNotEmpty()) View.VISIBLE else View.GONE
            tvNoResult.visibility = if (total > 0 && shown == 0) View.VISIBLE else View.GONE
            llEmpty.visibility = if (total == 0) View.VISIBLE else View.GONE
            if (isFoods) {
                tvEmptyTitle.text = "Chưa có món yêu thích"
                tvEmptyDesc.text = "Bấm vào biểu tượng trái tim trên món ăn để lưu lại nhé!"
            } else {
                tvEmptyTitle.text = "Chưa có nhà hàng yêu thích"
                tvEmptyDesc.text = "Bấm vào biểu tượng trái tim trên nhà hàng để lưu lại nhé!"
            }

            catButtons.forEachIndexed { i, btn ->
                val catName = SampleData.CATEGORIES.getOrNull(i) ?: ""
                val isActive = catName.equals(favCategory, ignoreCase = true)
                btn.setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                btn.setTextColor(if (isActive) Color.WHITE else activity.getColor(R.color.text_grey2))
                btn.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }

            val suggestions = if (isFoods && q.isEmpty() && favCategory == "Tất cả") suggestFoods(allFoods) else emptyList()
            suggestAdapter.submit(suggestions)
            tvSuggestTitle.text = if (allFoods.isEmpty()) "Món ngon đề xuất" else "Có thể bạn cũng thích"
            tvSuggestTitle.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE
            rvSuggest.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE
        }

        SampleData.CATEGORIES.forEach { cat ->
            val btn = Button(activity).apply {
                text = cat
                textSize = 14f
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, activity.dp(8), 0)
                layoutParams = lp
                setOnClickListener {
                    favCategory = cat
                    refresh()
                }
            }
            catButtons.add(btn)
            llCats.addView(btn)
        }

        tabFoods.setOnClickListener { favTab = "foods"; refresh() }
        tabRestaurants.setOnClickListener { favTab = "restaurants"; refresh() }

        etSearch.setText(favSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                favSearch = s?.toString().orEmpty()
                refresh()
            }
        })
        etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        btnSort.setOnClickListener {
            val sorts = if (favTab == "foods") foodSorts else restaurantSorts
            AlertDialog.Builder(activity)
                .setTitle("Sắp xếp theo")
                .setSingleChoiceItems(
                    sorts.map { it.label }.toTypedArray(),
                    sorts.indexOf(favSort)
                ) { dialog, which ->
                    favSort = sorts[which]
                    dialog.dismiss()
                    refresh()
                }
                .show()
        }

        btnAddAll.setOnClickListener {
            if (shownFoods.isEmpty()) {
                activity.showToast("Không có món nào để thêm")
            } else {
                shownFoods.forEach { CartManager.addFood(it) }
                activity.showToast("Đã thêm ${shownFoods.size} món vào giỏ hàng 🛒")
            }
        }

        btnClear.setOnClickListener {
            val isFoods = favTab == "foods"
            val count = if (isFoods) FavoriteManager.foodCount() else FavoriteManager.restaurantCount()
            if (count == 0) return@setOnClickListener

            AlertDialog.Builder(activity)
                .setTitle("Xóa yêu thích")
                .setMessage(
                    if (isFoods) "Bạn muốn xóa tất cả $count món khỏi Yêu thích?"
                    else "Bạn muốn xóa tất cả $count nhà hàng khỏi Yêu thích?"
                )
                .setPositiveButton("Xóa hết") { _, _ ->
                    if (isFoods) {
                        val backup = FavoriteManager.clearFoods()
                        activity.showUndoSnackbar("Đã xóa $count món khỏi Yêu thích") {
                            FavoriteManager.restoreFoods(backup)
                        }
                    } else {
                        val backup = FavoriteManager.clearRestaurants()
                        activity.showUndoSnackbar("Đã xóa $count nhà hàng khỏi Yêu thích") {
                            FavoriteManager.restoreRestaurants(backup)
                        }
                    }
                }
                .setNegativeButton("Không", null)
                .show()
        }

        btnExplore.setOnClickListener { activity.selectTab("home") }

        onRefreshRequested = { refresh() }
        refresh()
    }
}
