package com.example.timetable.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.model.Course
import com.example.timetable.model.formatActiveWeeks
import com.example.timetable.model.parseActiveWeeks
import com.example.timetable.model.weekSchedulesOverlap

@Composable
fun AddCourseDialog(
    weekDays: List<String>,
    courses: List<Course>,
    maxSection: Int,
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onAddCourse: (Course) -> Unit
) {
    CourseEditorDialog(
        title = "添加课程",
        originalCourse = null,
        weekDays = weekDays,
        courses = courses,
        maxSection = maxSection,
        totalWeeks = totalWeeks,
        onDismiss = onDismiss,
        onSaveCourse = onAddCourse
    )
}

@Composable
fun EditCourseDialog(
    course: Course,
    weekDays: List<String>,
    courses: List<Course>,
    maxSection: Int,
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit,
    onSaveCourse: (Course) -> Unit
) {
    CourseEditorDialog(
        title = "编辑课程",
        originalCourse = course,
        weekDays = weekDays,
        courses = courses,
        maxSection = maxSection,
        totalWeeks = totalWeeks,
        onDismiss = onDismiss,
        onDeleteRequest = onDeleteRequest,
        onSaveCourse = onSaveCourse
    )
}

@Composable
private fun CourseEditorDialog(
    title: String,
    originalCourse: Course?,
    weekDays: List<String>,
    courses: List<Course>,
    maxSection: Int,
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onDeleteRequest: (() -> Unit)? = null,
    onSaveCourse: (Course) -> Unit
) {
    var name by remember(originalCourse) { mutableStateOf(originalCourse?.name.orEmpty()) }
    var teacher by remember(originalCourse) { mutableStateOf(originalCourse?.teacher.orEmpty()) }
    var classroom by remember(originalCourse) { mutableStateOf(originalCourse?.classroom.orEmpty()) }
    var weekDay by remember(originalCourse) { mutableStateOf(originalCourse?.weekDay.orEmpty()) }
    var startSection by remember(originalCourse) {
        mutableStateOf(originalCourse?.startSection?.toString().orEmpty())
    }
    var endSection by remember(originalCourse) {
        mutableStateOf(originalCourse?.endSection?.toString().orEmpty())
    }
    var activeWeeksText by remember(originalCourse, totalWeeks) {
        mutableStateOf(
            originalCourse?.let { formatActiveWeeks(it.activeWeeks) } ?: "1-$totalWeeks"
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CourseTextField(name, { name = it }, "课程名称")
                CourseTextField(teacher, { teacher = it }, "教师")
                CourseTextField(classroom, { classroom = it }, "教室")
                CourseTextField(weekDay, { weekDay = it }, "星期（例如：周一）")
                CourseTextField(
                    startSection,
                    { startSection = it.filter(Char::isDigit) },
                    "开始节次"
                )
                CourseTextField(
                    endSection,
                    { endSection = it.filter(Char::isDigit) },
                    "结束节次"
                )
                CourseTextField(
                    activeWeeksText,
                    { activeWeeksText = it },
                    "上课周次（如 1-9,11-18）"
                )

                errorMessage?.let {
                    Text(
                        text = it,
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
                    val activeWeeks = parseActiveWeeks(activeWeeksText, totalWeeks)
                    val editingIndex = originalCourse?.let(courses::indexOf) ?: -1

                    errorMessage = when {
                        name.isBlank() -> "请输入课程名称"
                        weekDay !in weekDays -> "星期只能填写周一至周日"
                        start == null || start !in 1..maxSection ->
                            "开始节次必须在 1 到 $maxSection 之间"
                        end == null || end !in start..maxSection ->
                            "结束节次必须在开始节次到第 $maxSection 节之间"
                        activeWeeks == null ->
                            "周次格式不正确，请填写 1-$totalWeeks 范围内的周次"
                        courses.withIndex().any { (index, existing) ->
                            index != editingIndex &&
                                existing.weekDay == weekDay &&
                                start <= existing.endSection &&
                                end >= existing.startSection &&
                                weekSchedulesOverlap(activeWeeks, existing)
                        } -> "该时间已经有其他课程"
                        else -> {
                            onSaveCourse(
                                Course(
                                    name = name.trim(),
                                    teacher = teacher.trim(),
                                    classroom = classroom.trim(),
                                    weekDay = weekDay,
                                    startSection = start,
                                    endSection = end,
                                    activeWeeks = activeWeeks
                                )
                            )
                            null
                        }
                    }
                }
            ) {
                Text(if (originalCourse == null) "添加" else "保存")
            }
        },
        dismissButton = {
            Row {
                onDeleteRequest?.let { delete ->
                    TextButton(onClick = delete) {
                        Text("删除课程", color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
private fun CourseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
fun DeleteCourseDialog(
    course: Course,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除课程") },
        text = { Text("确定要删除“${course.name}”吗？删除后无法撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text("确认删除", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
