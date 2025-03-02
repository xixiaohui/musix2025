package com.xxh.ringbones

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlin.reflect.KFunction2


class LandActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val size = currentWindowAdaptiveInfo().windowSizeClass
            Musix2025App(size)
        }
    }

}


private val alignYourBodyData = listOf(
    R.drawable.ab1_inversions to R.string.hindi_bollywood,
    R.drawable.ab1_inversions to R.string.tamil,
    R.drawable.ab1_inversions to R.string.sms,
    R.drawable.ab1_inversions to R.string.music,
    R.drawable.ab1_inversions to R.string.malayalam,
    R.drawable.ab1_inversions to R.string.funny,
    R.drawable.ab1_inversions to R.string.sound,
    R.drawable.ab1_inversions to R.string.miscellaneous,
    R.drawable.ab1_inversions to R.string.devotional,
    R.drawable.ab1_inversions to R.string.baby,
    R.drawable.ab1_inversions to R.string.iphone,

    ).map { DrawableStringPair(it.first, it.second) }

private val favoriteCollectionsData = listOf(
    R.drawable.ab1_inversions to R.string.hindi_bollywood,
    R.drawable.ab1_inversions to R.string.tamil,
    R.drawable.ab1_inversions to R.string.sms,
    R.drawable.ab1_inversions to R.string.music,
    R.drawable.ab1_inversions to R.string.malayalam,
    R.drawable.ab1_inversions to R.string.funny,
    R.drawable.ab1_inversions to R.string.sound,
    R.drawable.ab1_inversions to R.string.miscellaneous,
    R.drawable.ab1_inversions to R.string.devotional,
    R.drawable.ab1_inversions to R.string.baby,
    R.drawable.ab1_inversions to R.string.iphone,
).map { DrawableStringPair(it.first, it.second) }

private data class DrawableStringPair(
    @DrawableRes val drawable: Int,
    @StringRes val text: Int
)

@Composable
fun SearchBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { newText ->
            text = newText
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.background),

        leadingIcon= {
            IconButton(onClick = {
                onImeActionDone(context, text)
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            }

        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        placeholder = {
            Text(stringResource(R.string.placeholder_search))
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done  // 设置回车按钮为 "Done"
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                //当回车键按下时执行的操作
                onImeActionDone(context, text)
            }
        )
    )
}

//处理在搜索栏输入后，按下回车后的操作
fun onImeActionDone(context: Context, text: String) {
    navigateToSearchResultActivityText(context, text)
}


@Composable
fun AlignYourBodyElement(
    @DrawableRes drawable: Int,
    @StringRes text: Int,
    navigateToSearchResult: KFunction2<Context, Int, Unit>,
    index: Int,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    Column(
        modifier = modifier.clickable {
//            Log.v("musixAlignYourBodyElement", text.toString())

            navigateToSearchResult(context, index)

        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
        )
        Text(
            text = stringResource(text),
            modifier = Modifier.paddingFromBaseline(top = 24.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun FavoriteCollectionCard(
    @DrawableRes drawable: Int,
    @StringRes text: Int,
    navigateToSearchResult: KFunction2<Context, Int, Unit>,
    index: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(255.dp)
                .clickable {
                    navigateToSearchResult(context, index)
                }
        ) {
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview
@Composable
fun FavoriteCollectionCardPreview() {
    Musix2025Theme {
        FavoriteCollectionCard(
            text = R.string.ab1_inversions,
            drawable = R.drawable.ab1_inversions,
            navigateToSearchResult = ::navigateToSearchResultActivity,
            index = 0,
            modifier = Modifier.padding(8.dp)
        )
    }
}

fun navigateToSearchResultActivity(context: Context, index: Int) {
    val intent = Intent(context, SearchResultActivity::class.java).apply {
//                    putExtra("EXTRA_INFO", ringtone as Serializable)
        putExtra("EXTRA_INFO", index)
    }
    context.startActivity(intent)
}

fun navigateToSearchResultActivityText(context: Context, text: String) {
    val intent = Intent(context, SearchResultActivity::class.java).apply {
//                    putExtra("EXTRA_INFO", ringtone as Serializable)
        putExtra("EXTRA_INFO", 0)
        putExtra("EXTRA_TITLE", text)
    }
    context.startActivity(intent)
}

@Composable
fun AlignYourBodyRow(
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
    ) {
        itemsIndexed(alignYourBodyData) { index, item ->
            AlignYourBodyElement(item.drawable, item.text, ::navigateToSearchResultActivity, index)
        }
    }
}

@Composable
fun FavoriteCollectionsGrid(
    modifier: Modifier = Modifier
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.height(168.dp)
    ) {
        itemsIndexed(favoriteCollectionsData) { index, item ->
            FavoriteCollectionCard(
                item.drawable,
                item.text,
                ::navigateToSearchResultActivity,
                index,
                Modifier.height(80.dp)
            )
        }
    }
}

@Composable
fun HomeSection(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .paddingFromBaseline(top = 40.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp)
        )
        content()
    }
}


@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        SearchBar(Modifier.padding(horizontal = 16.dp))
        HomeSection(title = R.string.align_your_body) {
            AlignYourBodyRow()
        }
        HomeSection(title = R.string.favorite_collections) {
            FavoriteCollectionsGrid()
        }
        Spacer(Modifier.height(16.dp))
    }
}


@Composable
private fun SootheBottomNavigation(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null
                )
            },
            label = {
                Text(stringResource(R.string.bottom_navigation_home))
            },
            selected = true,
            onClick = {

                navigateToSearchResultActivity(context, 0)

            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null
                )
            },
            label = {
                Text(stringResource(R.string.bottom_navigation_profile))
            },
            selected = false,
            onClick = {
                navigateToSearchResultActivity(context, 0)
            }
        )
    }
}


@Composable
fun MySootheAppPortrait() {
    Musix2025Theme {
        Scaffold(
            bottomBar = { SootheBottomNavigation() }
        ) { padding ->
            HomeScreen(Modifier.padding(padding))
        }
    }
}


@Composable
private fun SootheNavigationRail(modifier: Modifier = Modifier) {
    NavigationRail(
        modifier = modifier.padding(start = 8.dp, end = 8.dp),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null
                    )
                },
                label = {
                    Text(stringResource(R.string.bottom_navigation_home))
                },
                selected = true,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null
                    )
                },
                label = {
                    Text(stringResource(R.string.bottom_navigation_profile))
                },
                selected = false,
                onClick = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MySootheAppLandscape() {
    Musix2025Theme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 5.dp
        ) {
            Row {
                SootheNavigationRail()
                HomeScreen()
            }
        }
    }
}


@Composable
fun Musix2025App(windowSize: WindowSizeClass) {
    when (windowSize.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            MySootheAppPortrait()
        }

        WindowWidthSizeClass.EXPANDED -> {
            MySootheAppLandscape()
        }
    }
}




