package com.example.timetable.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.model.Course
import com.example.timetable.importer.TimetableImportSchool
import com.example.timetable.model.formatActiveWeeks
import com.example.timetable.model.isActiveInWeek

@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val courses by viewModel.courses.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val pdfImportState by viewModel.pdfImportState.collectAsState()
    var selectedImportSchool by remember { mutableStateOf<TimetableImportSchool?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val school = selectedImportSchool
        if (uri != null && school != null) {
            viewModel.parseTimetableFile(uri, school)
        }
        selectedImportSchool = null
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSectionCountDialog by remember { mutableStateOf(false) }
    var showTimeSettingsDialog by remember { mutableStateOf(false) }
    var showSemesterSettingsDialog by remember { mutableStateOf(false) }
    var showImportSchoolDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var coursePendingDeletion by remember { mutableStateOf<Course?>(null) }
    var currentWeek by remember { mutableStateOf(1) }
    val sectionCount = settings.sectionCount
    val semesterStart = settings.semesterStart
    val totalWeeks = settings.totalWeeks
    val classPeriods = settings.classPeriods

    LaunchedEffect(totalWeeks) {
        currentWeek = currentWeek.coerceIn(1, totalWeeks)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "我的课表",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Box {
                TextButton(onClick = { showTopMenu = true }) {
                    Text(
                        text = "⋮",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(
                    expanded = showTopMenu,
                    onDismissRequest = { showTopMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("添加课程") },
                        onClick = {
                            showTopMenu = false
                            showAddDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("从课表导入") },
                        onClick = {
                            showTopMenu = false
                            showImportSchoolDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("设置每日节数") },
                        onClick = {
                            showTopMenu = false
                            showSectionCountDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("设置节次时间") },
                        onClick = {
                            showTopMenu = false
                            showTimeSettingsDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("学期设置") },
                        onClick = {
                            showTopMenu = false
                            showSemesterSettingsDialog = true
                        }
                    )
                }
            }
        }

        if (showImportSchoolDialog) {
            ImportSchoolDialog(
                schools = TimetableImportSchool.entries,
                onDismiss = { showImportSchoolDialog = false },
                onSelect = { school ->
                    showImportSchoolDialog = false
                    selectedImportSchool = school
                    pdfPicker.launch(school.acceptedMimeTypes)
                }
            )
        }

        when (val state = pdfImportState) {
            PdfImportState.Idle -> Unit
            PdfImportState.Loading -> AlertDialog(
                onDismissRequest = {},
                title = { Text("正在解析课表") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
                        Text("正在本地读取 PDF，请稍候……")
                    }
                },
                confirmButton = {}
            )
            PdfImportState.Saving -> AlertDialog(
                onDismissRequest = {},
                title = { Text("正在导入课程") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
                        Text("正在保存所选课程，请稍候……")
                    }
                },
                confirmButton = {}
            )
            is PdfImportState.Error -> AlertDialog(
                onDismissRequest = viewModel::dismissPdfImportResult,
                title = { Text("无法识别课表") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissPdfImportResult) { Text("确定") }
                }
            )
            is PdfImportState.Success -> PdfRecognitionResultDialog(
                result = state.timetable,
                existingCourses = courses,
                onDismiss = viewModel::dismissPdfImportResult,
                onImport = viewModel::importCourses
            )
            is PdfImportState.Completed -> AlertDialog(
                onDismissRequest = viewModel::dismissPdfImportResult,
                title = { Text("导入完成") },
                text = { Text("已成功导入 ${state.importedCount} 门课程。") },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissPdfImportResult) { Text("完成") }
                }
            )
        }

        if (showAddDialog) {
            AddCourseDialog(
                weekDays = weekDays,
                courses = courses,
                maxSection = sectionCount,
                totalWeeks = totalWeeks,
                onDismiss = { showAddDialog = false },
                onAddCourse = { course ->
                    viewModel.addCourse(course)
                    showAddDialog = false
                }
            )
        }

        if (showSectionCountDialog) {
            SectionCountDialog(
                currentSectionCount = sectionCount,
                minimumSectionCount = courses.maxOfOrNull { it.endSection } ?: 1,
                onDismiss = { showSectionCountDialog = false },
                onConfirm = { newSectionCount ->
                    val newPeriods = classPeriods.take(newSectionCount).toMutableList()
                    while (newPeriods.size < newSectionCount) {
                        val previousEnd = newPeriods.lastOrNull()?.endMinutes ?: (8 * 60 - 10)
                        val number = newPeriods.size + 1
                        val startMinutes = previousEnd + 10
                        newPeriods.add(
                            com.example.timetable.model.ClassPeriod(
                                number = number,
                                startMinutes = startMinutes,
                                endMinutes = startMinutes + 45
                            )
                        )
                    }
                    viewModel.saveSettings(
                        settings.copy(
                            sectionCount = newSectionCount,
                            classPeriods = newPeriods
                        )
                    )
                    showSectionCountDialog = false
                }
            )
        }

        if (showTimeSettingsDialog) {
            TimeSettingsDialog(
                currentPeriods = classPeriods,
                onDismiss = { showTimeSettingsDialog = false },
                onConfirm = { newPeriods ->
                    viewModel.saveSettings(
                        settings.copy(classPeriods = newPeriods)
                    )
                    showTimeSettingsDialog = false
                }
            )
        }

        selectedCourse?.let { courseToEdit ->
            EditCourseDialog(
                course = courseToEdit,
                weekDays = weekDays,
                courses = courses,
                maxSection = sectionCount,
                totalWeeks = totalWeeks,
                onDismiss = { selectedCourse = null },
                onDeleteRequest = {
                    coursePendingDeletion = courseToEdit
                    selectedCourse = null
                },
                onSaveCourse = { updatedCourse ->
                    val courseIndex = courses.indexOf(courseToEdit)
                    if (courseIndex >= 0) {
                        viewModel.updateCourse(
                            updatedCourse.copy(id = courseToEdit.id)
                        )
                    }
                    selectedCourse = null
                }
            )
        }

        coursePendingDeletion?.let { courseToDelete ->
            DeleteCourseDialog(
                course = courseToDelete,
                onDismiss = { coursePendingDeletion = null },
                onConfirmDelete = {
                    viewModel.deleteCourse(courseToDelete)
                    coursePendingDeletion = null
                }
            )
        }

        if (showSemesterSettingsDialog) {
            SemesterSettingsDialog(
                currentStartDate = semesterStart,
                currentTotalWeeks = totalWeeks,
                minimumTotalWeeks = courses.maxOfOrNull { it.endWeek } ?: 1,
                onDismiss = { showSemesterSettingsDialog = false },
                onConfirm = { newStartDate, newTotalWeeks ->
                    viewModel.saveSettings(
                        settings.copy(
                            semesterStart = newStartDate,
                            totalWeeks = newTotalWeeks
                        )
                    )
                    currentWeek = currentWeek.coerceIn(1, newTotalWeeks)
                    showSemesterSettingsDialog = false
                }
            )
        }

        val displayedWeekStart = semesterStart.plusWeeks((currentWeek - 1).toLong())
        val displayedWeekEnd = displayedWeekStart.plusDays(6)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(enabled = currentWeek > 1, onClick = { currentWeek-- }) {
                Text("上一周")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "第 $currentWeek 周", fontWeight = FontWeight.Bold)
                Text(
                    text = "${formatDate(displayedWeekStart)}—${formatDate(displayedWeekEnd)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            TextButton(enabled = currentWeek < totalWeeks, onClick = { currentWeek++ }) {
                Text("下一周")
            }
        }

        Text(
            text = "课程数量：${courses.size} · 每日 $sectionCount 节 · 共 $totalWeeks 周",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                TimetableHeaderCell(text = "节次", modifier = Modifier.weight(0.85f))
                for ((dayIndex, day) in weekDays.withIndex()) {
                    val date = displayedWeekStart.plusDays(dayIndex.toLong())
                    TimetableHeaderCell(
                        text = "$day\n${formatMonthDay(date)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            for (section in 1..sectionCount) {
                val period = classPeriods[section - 1]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                ) {
                    TimetableSectionCell(
                        section = section,
                        startTime = formatMinutesAsTime(period.startMinutes),
                        endTime = formatMinutesAsTime(period.endMinutes),
                        modifier = Modifier.weight(0.85f)
                    )
                    for (day in weekDays) {
                        val course = courses.find {
                            it.weekDay == day &&
                                section in it.startSection..it.endSection &&
                                it.isActiveInWeek(currentWeek)
                        }
                        TimetableCourseCell(
                            course = course,
                            section = section,
                            onCourseClick = { selectedCourse = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSchoolDialog(
    schools: List<TimetableImportSchool>,
    onDismiss: () -> Unit,
    onSelect: (TimetableImportSchool) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择学校") },
        text = {
            Column {
                Text("请选择课表所属学校。后续可以继续添加其他学校。")
                schools.forEach { school ->
                    TextButton(
                        onClick = { onSelect(school) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(school.displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PdfRecognitionResultDialog(
    result: com.example.timetable.importer.ParsedTimetable,
    existingCourses: List<Course>,
    onDismiss: () -> Unit,
    onImport: (List<Course>) -> Unit
) {
    val duplicateIndices = remember(result, existingCourses) {
        result.courses.indices.filterTo(mutableSetOf()) { index ->
            existingCourses.any { existing ->
                coursesAreDuplicates(result.courses[index], existing)
            }
        }
    }
    val conflictIndices = remember(result, existingCourses) {
        result.courses.indices.filterTo(mutableSetOf()) { index ->
            val course = result.courses[index]
            existingCourses.any { existing -> coursesConflict(course, existing) } ||
                result.courses.indices.any { otherIndex ->
                    otherIndex != index && coursesConflict(course, result.courses[otherIndex])
                }
        }
    }
    var selectedIndices by remember(result, duplicateIndices) {
        mutableStateOf(result.courses.indices.filterNot(duplicateIndices::contains).toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("识别到 ${result.courses.size} 门课程") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                result.semester?.let { Text(it, fontWeight = FontWeight.Bold) }
                Text(
                    "请选择需要导入的课程。重复课程已自动取消选择。",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                result.courses.forEachIndexed { index, course ->
                    val duplicate = index in duplicateIndices
                    val conflict = index in conflictIndices && !duplicate
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = index in selectedIndices,
                            enabled = !duplicate,
                            onCheckedChange = { checked ->
                                selectedIndices = if (checked) {
                                    selectedIndices + index
                                } else {
                                    selectedIndices - index
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(course.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${course.weekDay} ${course.startSection}-${course.endSection}节 · " +
                                    "${formatActiveWeeks(course.activeWeeks)}周"
                            )
                            Text("${course.classroom} · ${course.teacher}", color = Color.Gray)
                            when {
                                duplicate -> Text("已存在，跳过", color = Color.Gray, fontSize = 12.sp)
                                conflict -> Text("与其他课程时间冲突", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
                result.warnings.forEach { warning ->
                    Text("提示：$warning", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedIndices.isNotEmpty(),
                onClick = {
                    onImport(selectedIndices.sorted().map(result.courses::get))
                }
            ) {
                Text("导入所选（${selectedIndices.size}）")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun coursesAreDuplicates(first: Course, second: Course): Boolean =
    first.name == second.name &&
        first.weekDay == second.weekDay &&
        first.startSection == second.startSection &&
        first.endSection == second.endSection &&
        first.activeWeeks == second.activeWeeks

private fun coursesConflict(first: Course, second: Course): Boolean =
    first.weekDay == second.weekDay &&
        first.startSection <= second.endSection &&
        first.endSection >= second.startSection &&
        first.activeWeeks.any(second.activeWeeks::contains) &&
        !coursesAreDuplicates(first, second)

@Composable
private fun TimetableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(Color(0xFFE8EAF6), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimetableSectionCell(
    section: Int,
    startTime: String,
    endTime: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "第${section}节", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = startTime, fontSize = 8.sp)
            Text(text = endTime, fontSize = 8.sp)
        }
    }
}

@Composable
private fun TimetableCourseCell(
    course: Course?,
    section: Int,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
            .background(
                if (course == null) Color.White else Color(0xFFC5CAE9),
                RoundedCornerShape(6.dp)
            )
            .clickable(enabled = course != null) { course?.let(onCourseClick) }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (course != null && section == course.startSection) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = course.name, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = course.classroom, fontSize = 8.sp)
                Text(text = course.teacher, fontSize = 8.sp)
                Text(text = "${course.startSection}-${course.endSection}节", fontSize = 8.sp)
            }
        }
    }
}
