import { useState, useMemo } from 'react';
import type {
  LeaderboardResponse,
  BadgeResponseDto,
  UserBadgeResponseDto,
  SubjectResponse,
} from '@/types/gamification.types';
import { Podium } from './components/Podium';
import { LeaderboardTable } from './components/LeaderboardTable';
import { BadgeShowcase } from './components/BadgeShowcase';
import { Trophy, BookOpen, Award, Sparkles, Info, Search, Check } from 'lucide-react';

// Danh sách môn học trong hệ thống (/public/subjects)
const MOCK_SUBJECTS: SubjectResponse[] = [
  { id: 1, code: 'IT3040', name: 'Kỹ thuật lập trình' },
  { id: 2, code: 'MI1111', name: 'Giải tích 1' },
  { id: 3, code: 'IT3080', name: 'Mạng máy tính' },
  { id: 4, code: 'MI1141', name: 'Đại số tuyến tính' },
  { id: 5, code: 'IT3120', name: 'Hệ quản trị CSDL' },
  { id: 6, code: 'PH1110', name: 'Vật lý đại cương 1' },
];

// Dữ liệu mẫu Bảng xếp hạng Học kỳ (LeaderboardResponse.java)
const MOCK_SEMESTER_DATA: LeaderboardResponse = {
  myRank: {
    rank: 12,
    totalPoints: 720,
    topPercent: 4.8,
    totalParticipants: 1540,
    fullName: 'Bạn (Sinh viên)',
    avatarUrl: undefined,
  },
  entries: [
    {
      rank: 1,
      userId: 101,
      userCode: '20210001',
      fullName: 'Nguyễn Văn An',
      totalPoints: 2450,
      isCurrentUser: false,
    },
    {
      rank: 2,
      userId: 102,
      userCode: '20210452',
      fullName: 'Trần Thị Bình',
      totalPoints: 2180,
      isCurrentUser: false,
    },
    {
      rank: 3,
      userId: 103,
      userCode: '20211290',
      fullName: 'Lê Quang Cường',
      totalPoints: 1920,
      isCurrentUser: false,
    },
    {
      rank: 4,
      userId: 104,
      userCode: '20212384',
      fullName: 'Phạm Duy Dũng',
      totalPoints: 1680,
      isCurrentUser: false,
    },
    {
      rank: 5,
      userId: 105,
      userCode: '20213120',
      fullName: 'Hoàng Hải Yến',
      totalPoints: 1450,
      isCurrentUser: false,
    },
    {
      rank: 6,
      userId: 106,
      userCode: '20214055',
      fullName: 'Vũ Minh Phúc',
      totalPoints: 1310,
      isCurrentUser: false,
    },
    {
      rank: 7,
      userId: 107,
      userCode: '20215112',
      fullName: 'Đặng Thanh Giang',
      totalPoints: 1150,
      isCurrentUser: false,
    },
    {
      rank: 12,
      userId: 999,
      userCode: '20219999',
      fullName: 'Bạn (Sinh viên)',
      totalPoints: 720,
      isCurrentUser: true,
    },
  ],
};

// Dữ liệu mẫu Bảng xếp hạng theo Môn học (LeaderboardPeriod.SUBJECT)
const MOCK_SUBJECT_DATA_MAP: Record<number, LeaderboardResponse> = {
  1: {
    // IT3040 - Kỹ thuật lập trình
    myRank: {
      rank: 4,
      totalPoints: 520,
      topPercent: 3.2,
      totalParticipants: 185,
      fullName: 'Bạn (Sinh viên)',
    },
    entries: [
      {
        rank: 1,
        userId: 103,
        userCode: '20211290',
        fullName: 'Lê Quang Cường',
        totalPoints: 890,
        isCurrentUser: false,
      },
      {
        rank: 2,
        userId: 101,
        userCode: '20210001',
        fullName: 'Nguyễn Văn An',
        totalPoints: 810,
        isCurrentUser: false,
      },
      {
        rank: 3,
        userId: 105,
        userCode: '20213120',
        fullName: 'Hoàng Hải Yến',
        totalPoints: 670,
        isCurrentUser: false,
      },
      {
        rank: 4,
        userId: 999,
        userCode: '20219999',
        fullName: 'Bạn (Sinh viên)',
        totalPoints: 520,
        isCurrentUser: true,
      },
      {
        rank: 5,
        userId: 102,
        userCode: '20210452',
        fullName: 'Trần Thị Bình',
        totalPoints: 490,
        isCurrentUser: false,
      },
      {
        rank: 6,
        userId: 104,
        userCode: '20212384',
        fullName: 'Phạm Duy Dũng',
        totalPoints: 440,
        isCurrentUser: false,
      },
    ],
  },
  2: {
    // MI1111 - Giải tích 1
    myRank: {
      rank: 15,
      totalPoints: 260,
      topPercent: 8.5,
      totalParticipants: 320,
      fullName: 'Bạn (Sinh viên)',
    },
    entries: [
      {
        rank: 1,
        userId: 102,
        userCode: '20210452',
        fullName: 'Trần Thị Bình',
        totalPoints: 980,
        isCurrentUser: false,
      },
      {
        rank: 2,
        userId: 104,
        userCode: '20212384',
        fullName: 'Phạm Duy Dũng',
        totalPoints: 840,
        isCurrentUser: false,
      },
      {
        rank: 3,
        userId: 106,
        userCode: '20214055',
        fullName: 'Vũ Minh Phúc',
        totalPoints: 720,
        isCurrentUser: false,
      },
      {
        rank: 4,
        userId: 101,
        userCode: '20210001',
        fullName: 'Nguyễn Văn An',
        totalPoints: 690,
        isCurrentUser: false,
      },
      {
        rank: 15,
        userId: 999,
        userCode: '20219999',
        fullName: 'Bạn (Sinh viên)',
        totalPoints: 260,
        isCurrentUser: true,
      },
    ],
  },
};

// Fallback mẫu cho các môn khác
const DEFAULT_FALLBACK_SUBJECT_DATA: LeaderboardResponse = {
  myRank: {
    rank: 8,
    totalPoints: 340,
    topPercent: 5.6,
    totalParticipants: 142,
    fullName: 'Bạn (Sinh viên)',
  },
  entries: [
    {
      rank: 1,
      userId: 101,
      userCode: '20210001',
      fullName: 'Nguyễn Văn An',
      totalPoints: 650,
      isCurrentUser: false,
    },
    {
      rank: 2,
      userId: 103,
      userCode: '20211290',
      fullName: 'Lê Quang Cường',
      totalPoints: 580,
      isCurrentUser: false,
    },
    {
      rank: 3,
      userId: 102,
      userCode: '20210452',
      fullName: 'Trần Thị Bình',
      totalPoints: 510,
      isCurrentUser: false,
    },
    {
      rank: 8,
      userId: 999,
      userCode: '20219999',
      fullName: 'Bạn (Sinh viên)',
      totalPoints: 340,
      isCurrentUser: true,
    },
  ],
};

// Danh mục tất cả huy hiệu hệ thống (BadgeResponseDto.java)
const MOCK_ALL_BADGES: BadgeResponseDto[] = [
  {
    id: 1,
    name: 'Chiến Thần Đề Thi HUST',
    description: 'Đóng góp trên 30 câu hỏi và đề thi được giảng viên thẩm định phê duyệt.',
    badgeTier: 'PLATINUM',
    badgeTriggerEvent: 'APPROVED_QUESTIONS',
    totalEarnedUsers: 24,
  },
  {
    id: 2,
    name: 'Thần Tốc Giải Đề',
    description: 'Hoàn thành 50 đề thi thử với độ chính xác trên 85%.',
    badgeTier: 'GOLD',
    badgeTriggerEvent: 'STUDY_STREAK',
    totalEarnedUsers: 68,
  },
  {
    id: 3,
    name: 'Hiệp Sĩ Bình Luận',
    description: 'Có 100 lời giải thích đề thi được cộng đồng sinh viên đánh giá hữu ích.',
    badgeTier: 'SILVER',
    badgeTriggerEvent: 'PUBLIC_POINTS',
    totalEarnedUsers: 142,
  },
  {
    id: 4,
    name: 'Quán Quân Học Kỳ',
    description: 'Đạt danh hiệu Top 3 Bảng xếp hạng điểm cống hiến toàn trường cuối kỳ.',
    badgeTier: 'PLATINUM',
    badgeTriggerEvent: 'LEADERBOARD_RANK',
    totalEarnedUsers: 6,
  },
  {
    id: 5,
    name: 'Ong Vàng Chăm Chỉ',
    description: 'Duy trì đăng nhập và ôn tập liên tục trong 30 ngày.',
    badgeTier: 'BRONZE',
    badgeTriggerEvent: 'STUDY_STREAK',
    totalEarnedUsers: 310,
  },
  {
    id: 6,
    name: 'Người Tiên Phong',
    description: 'Đề xuất thành công 10 bộ đề trắc nghiệm cho môn học mới.',
    badgeTier: 'GOLD',
    badgeTriggerEvent: 'PROPOSED_QUESTIONS',
    totalEarnedUsers: 45,
  },
];

// Danh sách huy hiệu mà người dùng đã đạt (UserBadgeResponseDto.java)
const MOCK_USER_BADGES: UserBadgeResponseDto[] = [
  {
    badgeId: 1,
    name: 'Chiến Thần Đề Thi HUST',
    description: 'Đóng góp trên 30 câu hỏi và đề thi được giảng viên thẩm định phê duyệt.',
    badgeTier: 'PLATINUM',
    earnedAt: '2026-08-15T08:30:00Z',
  },
  {
    badgeId: 2,
    name: 'Thần Tốc Giải Đề',
    description: 'Hoàn thành 50 đề thi thử với độ chính xác trên 85%.',
    badgeTier: 'GOLD',
    earnedAt: '2026-08-28T14:15:00Z',
  },
  {
    badgeId: 3,
    name: 'Hiệp Sĩ Bình Luận',
    description: 'Có 100 lời giải thích đề thi được cộng đồng sinh viên đánh giá hữu ích.',
    badgeTier: 'SILVER',
    earnedAt: '2026-09-02T19:45:00Z',
  },
];

export function LeaderboardPage() {
  const [activeTab, setActiveTab] = useState<'SEMESTER' | 'SUBJECT' | 'BADGES'>('SEMESTER');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number>(1);
  const [subjectSearchQuery, setSubjectSearchQuery] = useState('');

  const currentSubject = useMemo(
    () => MOCK_SUBJECTS.find((s) => s.id === selectedSubjectId) || MOCK_SUBJECTS[0],
    [selectedSubjectId]
  );

  const filteredSubjects = useMemo(() => {
    if (!subjectSearchQuery.trim()) return MOCK_SUBJECTS;
    const q = subjectSearchQuery.toLowerCase();
    return MOCK_SUBJECTS.filter(
      (s) => s.code.toLowerCase().includes(q) || s.name.toLowerCase().includes(q)
    );
  }, [subjectSearchQuery]);

  const currentData =
    activeTab === 'SUBJECT'
      ? MOCK_SUBJECT_DATA_MAP[selectedSubjectId] || DEFAULT_FALLBACK_SUBJECT_DATA
      : MOCK_SEMESTER_DATA;

  const top3Users = currentData.entries.slice(0, 3);
  const restUsers = currentData.entries.slice(3);

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Hero Banner Header */}
      <div className="relative overflow-hidden rounded-3xl bg-linear-to-r from-indigo-950 via-indigo-900 to-slate-900 text-white p-6 sm:p-8 shadow-xl border border-indigo-800/60">
        {/* Background decorative elements */}
        <div className="absolute -right-10 -bottom-10 w-72 h-72 bg-indigo-500/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute left-1/3 -top-10 w-60 h-60 bg-amber-500/15 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2 max-w-2xl">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-800/80 border border-indigo-600/50 text-indigo-200 text-xs font-semibold">
              <Sparkles className="w-3.5 h-3.5 text-amber-400" />
              <span>Hệ Thống Vinh Danh Tự Động SQB</span>
            </div>

            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight">
              Đấu Trường Vinh Danh & Xếp Hạng
            </h1>

            <p className="text-xs sm:text-sm text-indigo-200/90 leading-relaxed">
              Vinh danh những sinh viên có đóng góp xuất sắc nhất cho Ngân hàng Đề thi Đại học Bách Khoa Hà Nội.
              Tự động tích điểm cống hiến qua từng câu hỏi được duyệt và giải đáp chuyên môn!
            </p>
          </div>

          {/* Reward Rules Capsule */}
          <div className="shrink-0 p-4 bg-white/10 backdrop-blur-md rounded-2xl border border-white/15 space-y-2 text-xs min-w-[240px]">
            <div className="flex items-center gap-2 text-amber-300 font-bold">
              <Trophy className="w-4 h-4" />
              <span>
                {activeTab === 'SUBJECT'
                  ? `Vinh danh Môn (${currentSubject.code}):`
                  : 'Cơ cấu thưởng cuối kỳ:'}
              </span>
            </div>

            {activeTab === 'SUBJECT' ? (
              <ul className="space-y-1 text-slate-200 font-medium text-[11px]">
                <li className="flex items-center justify-between gap-4">
                  <span>🥇 Hạng 1 môn:</span>
                  <strong className="text-amber-400 font-bold">Thủ Khoa Môn</strong>
                </li>
                <li className="flex items-center justify-between gap-4">
                  <span>🥈 Hạng 2 môn:</span>
                  <strong className="text-slate-300 font-bold">Á Khoa Môn</strong>
                </li>
                <li className="flex items-center justify-between gap-4">
                  <span>🥉 Hạng 3 môn:</span>
                  <strong className="text-orange-400 font-bold">Kiện Tướng Môn</strong>
                </li>
              </ul>
            ) : (
              <ul className="space-y-1 text-slate-200 font-medium text-[11px]">
                <li className="flex items-center justify-between gap-4">
                  <span>🥇 Quán quân (Top 1):</span>
                  <strong className="text-amber-400 font-bold">+300 xu</strong>
                </li>
                <li className="flex items-center justify-between gap-4">
                  <span>🥈 Á quân (Top 2):</span>
                  <strong className="text-slate-300 font-bold">+180 xu</strong>
                </li>
                <li className="flex items-center justify-between gap-4">
                  <span>🥉 Quý quân (Top 3):</span>
                  <strong className="text-orange-400 font-bold">+90 xu</strong>
                </li>
              </ul>
            )}

            <div className="pt-1.5 border-t border-white/10 flex items-center gap-1.5 text-[10px] text-indigo-300">
              <Sparkles className="w-3 h-3 text-amber-400" />
              <span>
                {activeTab === 'SUBJECT'
                  ? 'Ghi danh học thuật & đề cử Mentor'
                  : 'Đầu tháng vinh danh: 30 / 20 / 10 xu'}
              </span>
            </div>
          </div>
        </div>

        {/* Tab Navigation Pill */}
        <div className="mt-8 pt-4 border-t border-indigo-800/60 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setActiveTab('SEMESTER')}
            className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeTab === 'SEMESTER'
                ? 'bg-white text-indigo-900 shadow-md shadow-black/20'
                : 'text-indigo-200 hover:bg-white/10'
            }`}
          >
            <Trophy className="w-4 h-4 text-amber-500" />
            Bảng Tổng Sắp Học Kỳ
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('SUBJECT')}
            className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeTab === 'SUBJECT'
                ? 'bg-white text-indigo-900 shadow-md shadow-black/20'
                : 'text-indigo-200 hover:bg-white/10'
            }`}
          >
            <BookOpen className="w-4 h-4 text-indigo-400" />
            Bảng Xếp Hạng Môn Học
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('BADGES')}
            className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeTab === 'BADGES'
                ? 'bg-white text-indigo-900 shadow-md shadow-black/20'
                : 'text-indigo-200 hover:bg-white/10'
            }`}
          >
            <Award className="w-4 h-4 text-purple-400" />
            Kho Huy Hiệu ({MOCK_ALL_BADGES.length})
          </button>
        </div>
      </div>

      {/* Dynamic Content based on Active Tab */}
      {activeTab === 'BADGES' ? (
        <BadgeShowcase allBadges={MOCK_ALL_BADGES} userBadges={MOCK_USER_BADGES} />
      ) : (
        <div className="space-y-6">
          {/* Subject Selector Bar when activeTab === 'SUBJECT' */}
          {activeTab === 'SUBJECT' && (
            <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 p-4 sm:p-5 shadow-xs space-y-3">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                    <BookOpen className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
                    <span>Chọn Môn Học Để Xem Bảng Xếp Hạng</span>
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                    Hệ thống tự động tổng hợp điểm cống hiến cho từng môn học trong kỳ 2026.1
                  </p>
                </div>

                {/* Search Box in Subject List */}
                <div className="relative w-full sm:w-64">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 dark:text-slate-500" />
                  <input
                    type="text"
                    placeholder="Tìm mã hoặc tên môn..."
                    value={subjectSearchQuery}
                    onChange={(e) => setSubjectSearchQuery(e.target.value)}
                    className="w-full pl-8.5 pr-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:bg-white dark:focus:bg-slate-800 focus:border-indigo-500 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 focus:ring-1 focus:ring-indigo-500/20 outline-none transition-all"
                  />
                </div>
              </div>

              {/* Horizontal Pill List of Subjects */}
              <div className="flex items-center gap-2 overflow-x-auto pb-1 pt-1 no-scrollbar">
                {filteredSubjects.map((sub) => {
                  const isSelected = sub.id === selectedSubjectId;
                  return (
                    <button
                      key={sub.id}
                      type="button"
                      onClick={() => setSelectedSubjectId(sub.id)}
                      className={`px-3.5 py-2 rounded-xl text-xs font-bold shrink-0 flex items-center gap-2 transition-all cursor-pointer border ${
                        isSelected
                          ? 'bg-indigo-900 text-white border-indigo-900 shadow-sm'
                          : 'bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-750 text-slate-700 dark:text-slate-200 border-slate-200/80 dark:border-slate-700'
                      }`}
                    >
                      <span className={`font-mono text-[11px] px-1.5 py-0.2 rounded ${
                        isSelected ? 'bg-indigo-800 text-indigo-200' : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
                      }`}>
                        {sub.code}
                      </span>
                      <span>{sub.name}</span>
                      {isSelected && <Check className="w-3.5 h-3.5 text-indigo-300 ml-0.5" />}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Section: Podium Top 3 */}
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 p-4 sm:p-6 shadow-xs">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-amber-500" />
                {activeTab === 'SEMESTER'
                  ? 'Bục Vinh Danh Học Kỳ 2026.1'
                  : `Bục Vinh Danh Môn: ${currentSubject.name} (${currentSubject.code})`}
              </h2>
              <div className="hidden sm:flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400">
                <Info className="w-4 h-4 text-slate-400 dark:text-slate-500" />
                <span>Cập nhật theo thời gian thực</span>
              </div>
            </div>

            {/* Render 3D-styled Podium */}
            <Podium topUsers={top3Users} scope={activeTab} />
          </div>

          {/* Section: Table for Rank 4 onward */}
          <div className="space-y-3">
            <h3 className="text-base font-bold text-slate-800 dark:text-slate-100 px-1">
              Bảng Xếp Hạng Chi Tiết {activeTab === 'SUBJECT' && `• Môn ${currentSubject.code}`}
            </h3>
            <LeaderboardTable
              entries={restUsers}
              myRank={currentData.myRank}
              scopeTitle={activeTab === 'SUBJECT' ? `môn ${currentSubject.code}` : undefined}
            />
          </div>
        </div>
      )}
    </div>
  );
}

