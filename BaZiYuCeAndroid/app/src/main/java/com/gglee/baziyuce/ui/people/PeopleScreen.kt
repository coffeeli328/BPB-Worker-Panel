package com.gglee.baziyuce.ui.people

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.data.PersonProfile
import com.gglee.baziyuce.engine.ShiChen
import com.gglee.baziyuce.ui.components.InlineNavTitle
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.CeladonSoft
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.Line
import com.gglee.baziyuce.ui.theme.ScreenBackground
import com.gglee.baziyuce.ui.theme.Slate

/** 多人档案列表：新增 / 切换 / 删除。 */
@Composable
fun PeopleScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
    onEditActive: () -> Unit,
    onOpenChart: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    ScreenBackground {
        Column(modifier = modifier.fillMaxSize()) {
            InlineNavTitle("档案")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AnimatedVisibility(visible = appeared, enter = fadeIn() + slideInVertically { 10 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "人员档案",
                            color = Ink,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                        )
                        Text(
                            "可为多人建立出生资料，切换后各自排盘与运势互不影响。",
                            color = Slate,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Celadon)
                        .clickable {
                            session.startNewPerson()
                            onEditActive()
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("新增人员", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }

                if (session.people.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.55f))
                            .border(1.dp, Line, RoundedCornerShape(14.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.PersonOutline, null, tint = CeladonSoft, modifier = Modifier.size(36.dp))
                            Text("暂无档案", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text("点「新增人员」填写出生信息并排盘", color = Slate, fontSize = 14.sp)
                        }
                    }
                } else {
                    session.people.forEach { person ->
                        PersonRow(
                            person = person,
                            selected = person.id == session.activePersonId,
                            onSelect = {
                                session.selectPerson(person.id)
                                onOpenChart()
                            },
                            onEdit = {
                                session.selectPerson(person.id)
                                onEditActive()
                            },
                            onDelete = { session.deletePerson(person.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonRow(
    person: PersonProfile,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shi = ShiChen.all[person.birthShiChenIndex.coerceIn(0, 11)]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = if (selected) 0.72f else 0.55f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Celadon.copy(alpha = 0.55f) else Line,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onSelect)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Celadon.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    person.name.take(1).ifEmpty { "?" },
                    color = Celadon,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        person.name,
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif,
                    )
                    if (selected) {
                        Spacer(Modifier.size(6.dp))
                        Icon(Icons.Outlined.CheckCircle, null, tint = Celadon, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    "${person.birthYear}年${person.birthMonth}月${person.birthDay}日 ${shi.branch.displayName}时 · ${person.gender.displayName}",
                    color = Slate,
                    fontSize = 13.sp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "编辑起盘",
                color = Celadon,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Text(
                "删除",
                color = Slate,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "删除",
                tint = Slate.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onDelete),
            )
        }
    }
}
