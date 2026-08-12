package com.example.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.ui.theme.ClassScheduleTheme

data class Course(
    val name: String,
    val teacher: String,
    val classroom: String,
    val weekDay: String,
    val startSection: Int,
    val endSection: Int
) {
    init {
        require(startSection in 1..12) { "开始节次必须在 1 到 12 之间" }
        require(endSection in startSection..12) { "结束节次不能早于开始节次" }
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClassScheduleTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Timetable(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Timetable(modifier: Modifier = Modifier) {

    val weekDays = listOf("周一", "周二", "周三", "周四", "周五")
    val courses = remember {
        mutableStateListOf(
            Course(
                name = "高等数学",
                teacher = "张老师",
                classroom = "A101",
                weekDay = "周一",
                startSection = 1,
                endSection = 2
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "我的课表",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("添加课程")
        }

        if (showAddDialog) {
            AddCourseDialog(
                weekDays = weekDays,
                courses = courses,
                onDismiss = { showAddDialog = false },
                onAddCourse = { course ->
                    courses.add(course)
                    showAddDialog = false
                }
            )
        }

        Text(
            text = "课程数量：${courses.size}",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 星期标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            TimetableHeaderCell(
                text = "节次",
                modifier = Modifier.weight(1f)
            )

            for (day in weekDays) {
                TimetableHeaderCell(
                    text = day,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 课程
        for (i in 1..6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
            ) {
                TimetableSectionCell(
                    section = i,
                    modifier = Modifier.weight(1f)
                )

                for (day in weekDays) {
                    val course = courses.find {
                        it.weekDay == day && i in it.startSection..it.endSection
                    }

                    TimetableCourseCell(
                        course = course,
                        section = i,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCourseDialog(
    weekDays: List<String>,
    courses: List<Course>,
    onDismiss: () -> Unit,
    onAddCourse: (Course) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var weekDay by remember { mutableStateOf("") }
    var startSection by remember { mutableStateOf("") }
    var endSection by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加课程") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("教师") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("教室") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = weekDay,
                    onValueChange = { weekDay = it },
                    label = { Text("星期（例如：周一）") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = startSection,
                    onValueChange = { startSection = it.filter(Char::isDigit) },
                    label = { Text("开始节次") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = endSection,
                    onValueChange = { endSection = it.filter(Char::isDigit) },
                    label = { Text("结束节次") },
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = startSection.toIntOrNull()
                    val end = endSection.toIntOrNull()

                    errorMessage = when {
                        name.isBlank() -> "请输入课程名称"
                        weekDay !in weekDays -> "星期只能填写周一至周五"
                        start == null || start !in 1..12 -> "开始节次必须在 1 到 12 之间"
                        end == null || end !in start..12 -> "结束节次不能早于开始节次"
                        courses.any {
                            it.weekDay == weekDay &&
                                start <= it.endSection &&
                                end >= it.startSection
                        } -> "该时间已经有其他课程"
                        else -> {
                            onAddCourse(
                                Course(
                                    name = name.trim(),
                                    teacher = teacher.trim(),
                                    classroom = classroom.trim(),
                                    weekDay = weekDay,
                                    startSection = start,
                                    endSection = end
                                )
                            )
                            null
                        }
                    }
                }
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TimetableHeaderCell(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(
                color = Color(0xFFE8EAF6),
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TimetableSectionCell(
    section: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "第${section}节",
            fontSize = 12.sp
        )
    }
}

@Composable
fun TimetableCourseCell(
    course: Course?,
    section: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = if (course == null) Color.White else Color(0xFFC5CAE9),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (course != null) {
            if (section == course.startSection) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = course.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = course.classroom,
                        fontSize = 9.sp
                    )
                    Text(
                        text = course.teacher,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "${course.startSection}-${course.endSection}节",
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
