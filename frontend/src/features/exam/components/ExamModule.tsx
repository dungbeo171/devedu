import { useEffect, useState } from 'react'
import { getExams, startExam } from '../api/examApi'
import type { ExamSession, ExamSummary } from '../types/exam'
import { ExamWorkspace } from './ExamWorkspace'
import { TeacherExamStudio } from './TeacherExamStudio'

export function ExamModule(){
  const[exams,setExams]=useState<ExamSummary[]>([]);const[session,setSession]=useState<ExamSession|null>(null);const[teacher,setTeacher]=useState(false);const[message,setMessage]=useState('')
  useEffect(()=>{if(teacher)return;setMessage('');void getExams().then(setExams).catch((e:unknown)=>{const code=e instanceof Error?e.message:'';setMessage(code==='AUTHENTICATION_REQUIRED'?'Đăng nhập bằng tài khoản sinh viên để xem kỳ thi.':code==='ROLE_REQUIRED'?'Danh sách này chỉ dành cho sinh viên.':code)})},[teacher])
  const join=async(slug:string)=>{setMessage('');try{setSession(await startExam(slug))}catch(e){setMessage(e instanceof Error?e.message:'Không thể bắt đầu kỳ thi.')}}
  if(session)return <ExamWorkspace initialSession={session} onBack={()=>setSession(null)}/>
  if(teacher)return <TeacherExamStudio onBack={()=>setTeacher(false)}/>
  return <section><div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="font-mono text-xs uppercase tracking-[.2em] text-blue-600">Assessment</p><h2 className="mt-2 text-2xl font-bold text-slate-900 sm:text-3xl">Kỳ thi lập trình</h2><p className="mt-2 text-sm leading-6 text-slate-600">Multiple Choice được chấm tự động; Coding được lưu để chấm sau.</p></div><button onClick={()=>setTeacher(true)} className="rounded-lg border border-blue-200 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:border-blue-500/50">Teacher Studio</button></div>
    {message?<p className="mt-5 rounded-xl border border-amber-400/20 bg-amber-400/5 p-4 text-sm text-amber-200">{message}</p>:null}<div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">{exams.map(exam=><article key={exam.id} className="flex min-h-60 flex-col rounded-2xl border border-blue-100 bg-white/60 p-5"><div className="flex items-center justify-between"><span className="rounded-full bg-violet-400/10 px-2.5 py-1 font-mono text-[11px] text-violet-300">EXAM</span><span className="text-xs text-slate-500">{exam.durationMinutes} phút</span></div><h3 className="mt-5 text-lg font-bold text-slate-900">{exam.title}</h3><p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-500">{exam.description}</p><p className="mt-4 text-xs text-slate-600">Bắt đầu: {new Date(exam.scheduledAt).toLocaleString('vi-VN')}</p><button onClick={()=>void join(exam.slug)} className="mt-auto rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-black text-white">Tham gia</button></article>)}</div></section>
}
