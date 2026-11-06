<template>
  <div class="student-header">
    <el-text class="mx-1" size="large">{{ studentName }}</el-text>
    <el-tag v-if="student" :type="student.enabled ? 'success' : 'danger'">
      {{ student.enabled ? 'Active' : 'Deactivated' }}
    </el-tag>
    <el-button
      v-if="student?.enabled"
      type="danger"
      plain
      :loading="deactivating"
      @click="confirmDeactivation()"
    >
      Deactivate
    </el-button>
  </div>
  <StudentActivities :studentId="studentId" />
  <el-divider />
  <StudentEvaluations :studentId="studentId" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import StudentActivities from '../activities/admin/StudentActivities.vue'
import StudentEvaluations from '../evaluations/admin/StudentEvaluations.vue'

import { useRoute } from 'vue-router'
import { deactivateStudent, findStudentById } from '@/apis/student'
import type { FindStudentByIdResponse, Student } from '@/apis/student/types'

const route = useRoute()

const studentId: number = Number(route.params.studentId)
const studentName = ref<string>('')
const student = ref<Student>()
const deactivating = ref(false)

async function loadStudent() {
  const result: FindStudentByIdResponse = await findStudentById(studentId)
  student.value = result.data
  studentName.value = result.data.firstName + ' ' + result.data.lastName
}

onMounted(loadStudent)

// UC-STU-deactivate-student steps 3-6. The dialog states the consequences (step 4); cancelling sends nothing (5a).
async function confirmDeactivation() {
  try {
    await ElMessageBox.confirm(
      `${studentName.value} will no longer be able to log in or submit weekly activity reports or peer evaluations, ` +
        'and any open session ends at the next request. The student stays on the team and past work is kept. ' +
        'Teammates will no longer evaluate this student. The account can be reactivated later; this is not deletion.',
      `Deactivate ${studentName.value}?`,
      { type: 'warning', confirmButtonText: 'Deactivate', cancelButtonText: 'Cancel' }
    )
  } catch {
    return // 5a: cancelled, the student is unchanged
  }

  deactivating.value = true
  try {
    const result = await deactivateStudent(studentId)
    student.value = result.data
    ElMessage.success(`${result.data.firstName} ${result.data.lastName} has been deactivated`)
  } catch (error) {
    // The shared interceptor has already shown the server's message (and routes 403 to the forbidden page).
    // On 409 (3a) the page was stale: reload so it shows Deactivated and no button.
    if (axios.isAxiosError(error) && error.response?.status === 409) {
      await loadStudent()
    }
  } finally {
    deactivating.value = false
  }
}
</script>

<style scoped>
.student-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
