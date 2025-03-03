package com.xxh.ringbones

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.unit.Dp
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlin.reflect.KFunction3


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
    R.drawable.erik to R.string.hindi_bollywood,
    R.drawable.james to R.string.tamil,
    R.drawable.artem to R.string.sms,
    R.drawable.dushawn to R.string.music,
    R.drawable.hanny to R.string.malayalam,
    R.drawable.james to R.string.funny,
    R.drawable.johanna to R.string.sound,
    R.drawable.leticia to R.string.miscellaneous,
    R.drawable.mohammad to R.string.devotional,
    R.drawable.simon to R.string.baby,
    R.drawable.anthony to R.string.iphone,

    ).map { DrawableStringPair(it.first, it.second) }

private val favoriteCollectionsData = listOf(
    R.drawable.james to R.string.funny,
    R.drawable.johanna to R.string.sound,
    R.drawable.leticia to R.string.miscellaneous,
    R.drawable.mohammad to R.string.devotional,
    R.drawable.simon to R.string.baby,
    R.drawable.anthony to R.string.iphone,
    R.drawable.erik to R.string.hindi_bollywood,
    R.drawable.james to R.string.tamil,
    R.drawable.artem to R.string.sms,
    R.drawable.dushawn to R.string.music,
    R.drawable.hanny to R.string.malayalam

).map { DrawableStringPair(it.first, it.second) }


//数据库type值和展示字符值对应
private val TypeNameToText = listOf(
    "Funny" to R.string.funny,
    "Devotional" to R.string.devotional,
    "Tamil" to R.string.tamil,
    "audio/mpeg" to R.string.audio_mpeg,
    "Iphone" to R.string.iphone,
    "Baby" to R.string.baby,
    "Sound Effects" to R.string.sound,
    "Music" to R.string.music,
    "Bollywood / Hindi" to R.string.hindi_bollywood,
    "SMS  / Message Alert" to R.string.sms,
    "Miscellaneous" to R.string.miscellaneous,
    "Malayalam" to R.string.malayalam,
).associate { it.second to it.first }


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

        leadingIcon = {
            IconButton(onClick = {
                onImeActionDone(context, text, true)
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
                onImeActionDone(context, text, true)
            }
        )
    )
}

//处理在搜索栏输入后，按下回车后的操作
fun onImeActionDone(context: Context, text: String, isSearch: Boolean) {
    navigateToSearchResultActivityText(context, text, isSearch)
}


@Composable
fun AlignYourBodyElement(
    @DrawableRes drawable: Int,
    @StringRes text: Int,
    navigate: KFunction3<Context, String, Boolean, Unit>,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    Column(
        modifier = modifier.clickable {
            Log.v("musixAlignYourBodyElement", TypeNameToText[text]!!)
            navigate(context, TypeNameToText[text]!!, false)
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
    navigate: KFunction3<Context, String, Boolean, Unit>,
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
                    navigate(context, TypeNameToText[text]!!, false)
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
            text = R.string.hindi_bollywood,
            drawable = R.drawable.erik,
            navigate = ::navigateToSearchResultActivityText,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Deprecated("no use index")
fun navigateToSearchResultActivity(context: Context, index: Int) {
    val intent = Intent(context, SearchResultActivity::class.java).apply {
        putExtra("EXTRA_INFO", index)
    }
    context.startActivity(intent)
}

fun navigateToSearchResultActivityText(context: Context, text: String, isSearch: Boolean) {
    val intent = Intent(context, SearchResultActivity::class.java).apply {

        if (isSearch) {
            putExtra("EXTRA_TITLE", text)
        } else {
            putExtra("EXTRA_TYPE", text)
        }

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
        items(alignYourBodyData) { item ->
            AlignYourBodyElement(item.drawable, item.text, ::navigateToSearchResultActivityText)
        }
    }
}

@Composable
fun FavoriteCollectionsGrid(
    modifier: Modifier = Modifier
) {
    val height = 84
    val fixedCount = 4
    val overallHeight = fixedCount * height

    LazyHorizontalGrid(
        rows = GridCells.Fixed(fixedCount),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.height(Dp(overallHeight.toFloat()))
    ) {
        items(favoriteCollectionsData) { item ->
            FavoriteCollectionCard(
                item.drawable,
                item.text,
                ::navigateToSearchResultActivityText,
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
        HomeSection(title = R.string.recommended_collections) {
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

            }
        )
    }
}


@Composable
fun MySootheAppPortrait() {
    Musix2025Theme {
        Scaffold(

            bottomBar = {
                if (BuildConfig.DEBUG) {
                    SootheBottomNavigation()
                }
            }

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




