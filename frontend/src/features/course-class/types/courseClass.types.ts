export interface CourseClassResponse {
  courseClassId: number;
  classCode: string;
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  semesterId: number;
  semesterName: string;
  activeSemester: boolean;
  lecturerId?: number;
  lecturerName?: string;
  totalStudents: number;
  createdAt: string;
}

export interface ClassStudentResponse {
  userId: number;
  studentCode: string;
  fullName: string;
  email: string;
  className?: string;
  schoolFaculty?: string;
  major?: string;
  dateOfBirth?: string;
  enrolledAt: string;
}

export interface LecturerSummaryResponse {
  id: number;
  fullName: string;
  email: string;
  studentLecturerCode?: string;
  schoolFaculty?: string;
}

export interface CreateCourseClassRequest {
  classCode: string;
  subjectId: number;
  lecturerId?: number;
}

export interface AssignStudentsRequest {
  studentCodes: string[];
}

export interface AssignStudentsResponse {
  totalEnrolled: number;
  alreadyEnrolledCount: number;
  notFoundCount: number;
  notFoundCodes: string[];
}
