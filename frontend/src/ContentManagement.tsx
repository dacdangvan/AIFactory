import { useEffect, useState } from "react";
import { Plus, RefreshCw, Save, Send, CheckCircle2, Globe } from "lucide-react";

type Product = { id:string; code:string; name:string };
type Article = { id:string; productId:string; contentType:string; code:string; slug:string; status:string; version:number; localeData:Record<string,any>; seoData:Record<string,any>; updatedAt:string };
type Props = { products:Product[]; username:string; password:string };

async function request(path:string, username:string, password:string, options:RequestInit={}) {
  const token=btoa(username+":"+password);
  const response=await fetch(path,{...options,headers:{"Content-Type":"application/json","Authorization":"Basic "+token,...(options.headers||{})}});
  const body=await response.json().catch(()=>null);
  if(!response.ok) throw new Error(body?.error?.message||body?.message||response.status+" "+response.statusText);
  return body?.data??body;
}
const emptyForm=()=>({contentType:"NEWS",code:"",slug:"",viTitle:"",viExcerpt:"",viBody:"",enTitle:"",enExcerpt:"",enBody:"",seoTitle:"",seoDescription:""});
export default function ContentManagement({products,username,password}:Props){
 const [productId,setProductId]=useState("");
 const [items,setItems]=useState<Article[]>([]);
 const [selected,setSelected]=useState<Article|null>(null);
 const [form,setForm]=useState(emptyForm());
 const [filter,setFilter]=useState("ALL");
 const [busy,setBusy]=useState(false);
 const [error,setError]=useState("");
 const [notice,setNotice]=useState("");
 useEffect(()=>{if(!productId&&products.length)setProductId(products[0].id)},[products,productId]);
 const load=async()=>{if(!productId)return;setBusy(true);setError("");try{const result=await request("/api/v1/content/articles?productId="+productId+"&limit=100",username,password);setItems(result||[])}catch(e:any){setError(e.message)}finally{setBusy(false)}};
 useEffect(()=>{load()},[productId]);
 const startNew=()=>{setSelected(null);setForm(emptyForm());setError("");setNotice("")};
 const edit=(a:Article)=>{setSelected(a);const vi=a.localeData?.["vi-VN"]||{},en=a.localeData?.["en-US"]||{};setForm({contentType:a.contentType,code:a.code,slug:a.slug,viTitle:vi.title||"",viExcerpt:vi.excerpt||"",viBody:vi.body||"",enTitle:en.title||"",enExcerpt:en.excerpt||"",enBody:en.body||"",seoTitle:a.seoData?.title||"",seoDescription:a.seoData?.description||""});setError("");setNotice("")};
 const save=async()=>{setBusy(true);setError("");setNotice("");try{
   const payload={contentType:form.contentType,code:form.code,slug:form.slug,localeData:{"vi-VN":{title:form.viTitle,excerpt:form.viExcerpt,body:form.viBody},"en-US":{title:form.enTitle,excerpt:form.enExcerpt,body:form.enBody}},seoData:{title:form.seoTitle,description:form.seoDescription}};
   const result=await request(selected?"/api/v1/content/articles/"+selected.id:"/api/v1/content/articles?productId="+productId,username,password,{method:selected?"PUT":"POST",body:JSON.stringify(payload)});
   setSelected(result);setNotice("Đã lưu nội dung phiên bản "+result.version);await load();
 }catch(e:any){setError(e.message)}finally{setBusy(false)}};
 const transition=async(action:"submit"|"approve"|"publish")=>{if(!selected)return;setBusy(true);setError("");setNotice("");try{const result=await request("/api/v1/content/articles/"+selected.id+"/"+action,username,password,{method:"POST"});setSelected(result);setNotice("Đã chuyển trạng thái: "+result.status);await load()}catch(e:any){setError(e.message)}finally{setBusy(false)}};
 const visible=items.filter(a=>filter==="ALL"||a.contentType===filter);
 return <div>
  <div className="stats"><div className="stat"><div className="stat-icon"><Globe/></div><div><small>Tổng nội dung</small><b>{items.length}</b></div></div><div className="stat"><div className="stat-icon"><Save/></div><div><small>Bản nháp</small><b>{items.filter(x=>x.status==="DRAFT").length}</b></div></div><div className="stat"><div className="stat-icon"><CheckCircle2/></div><div><small>Đã xuất bản</small><b>{items.filter(x=>x.status==="PUBLISHED").length}</b></div></div></div>
  {error&&<div className="alert error">{error}</div>}{notice&&<div className="alert success">{notice}</div>}
  <div className="panel"><div className="panel-head"><div><h2>Danh sách nội dung</h2><p>News · Investor Relations · Promotions · Guides</p></div><div style={{display:"flex",gap:8,alignItems:"center"}}><select value={productId} onChange={e=>setProductId(e.target.value)}>{products.map(p=><option key={p.id} value={p.id}>{p.code} - {p.name}</option>)}</select><select value={filter} onChange={e=>setFilter(e.target.value)}><option value="ALL">Tất cả loại</option><option value="NEWS">News</option><option value="INVESTOR_RELATIONS">Investor Relations</option><option value="PROMOTION">Promotions</option><option value="GUIDE">Guides</option></select><button className="icon-btn" onClick={load} title="Refresh"><RefreshCw size={16}/></button><button className="primary" style={{margin:0}} onClick={startNew}><Plus size={15}/> Tạo nội dung</button></div></div>
   <div className="table-wrap"><table><thead><tr><th>LOẠI</th><th>MÃ / SLUG</th><th>TIÊU ĐỀ (VI)</th><th>VERSION</th><th>TRẠNG THÁI</th><th>CẬP NHẬT</th></tr></thead><tbody>{visible.map(a=><tr key={a.id} onClick={()=>edit(a)} style={{cursor:"pointer"}}><td><span className="tag">{a.contentType}</span></td><td><b>{a.code}</b><small>{a.slug}</small></td><td>{a.localeData?.["vi-VN"]?.title||"—"}</td><td>v{a.version}</td><td><span className={"status "+a.status.toLowerCase()}><i/>{a.status}</span></td><td>{a.updatedAt?new Date(a.updatedAt).toLocaleString("vi-VN"):"—"}</td></tr>)}{!visible.length&&<tr><td colSpan={6} className="empty">Chưa có nội dung</td></tr>}</tbody></table></div>
  </div>
  <div className="panel"><div className="panel-head"><div><h2>{selected?"Chỉnh sửa nội dung":"Tạo nội dung mới"}</h2><p>{selected?"Phiên bản hiện tại: v"+selected.version+" · Trạng thái: "+selected.status:"Dữ liệu nội dung được quản lý theo phiên bản và locale."}</p></div></div>
   <div className="form-grid">
    <label className="field"><span>Content type</span><select value={form.contentType} onChange={e=>setForm({...form,contentType:e.target.value})}><option value="NEWS">News</option><option value="INVESTOR_RELATIONS">Investor Relations</option><option value="PROMOTION">Promotion</option><option value="GUIDE">Guide</option></select></label>
    <label className="field"><span>Code</span><input value={form.code} onChange={e=>setForm({...form,code:e.target.value})} placeholder="NEWS-2026-001" disabled={!!selected&&selected.status==="PUBLISHED"}/></label>
    <label className="field"><span>Slug</span><input value={form.slug} onChange={e=>setForm({...form,slug:e.target.value})} placeholder="tin-tuc-vib" disabled={!!selected&&selected.status==="PUBLISHED"}/></label>
    <label className="field"><span>Tiêu đề (vi-VN)</span><input value={form.viTitle} onChange={e=>setForm({...form,viTitle:e.target.value})}/></label>
    <label className="field"><span>Tiêu đề (en-US)</span><input value={form.enTitle} onChange={e=>setForm({...form,enTitle:e.target.value})}/></label>
    <label className="field"><span>SEO title</span><input value={form.seoTitle} onChange={e=>setForm({...form,seoTitle:e.target.value})}/></label>
    <label className="field"><span>Mô tả ngắn (vi-VN)</span><textarea rows={2} value={form.viExcerpt} onChange={e=>setForm({...form,viExcerpt:e.target.value})}/></label>
    <label className="field"><span>Short description (en-US)</span><textarea rows={2} value={form.enExcerpt} onChange={e=>setForm({...form,enExcerpt:e.target.value})}/></label>
    <label className="field"><span>SEO description</span><textarea rows={2} value={form.seoDescription} onChange={e=>setForm({...form,seoDescription:e.target.value})}/></label>
    <label className="field" style={{gridColumn:"1 / -1"}}><span>Nội dung chi tiết (vi-VN)</span><textarea rows={7} value={form.viBody} onChange={e=>setForm({...form,viBody:e.target.value})}/></label>
    <label className="field" style={{gridColumn:"1 / -1"}}><span>Full content (en-US)</span><textarea rows={7} value={form.enBody} onChange={e=>setForm({...form,enBody:e.target.value})}/></label>
   </div>
   <div style={{display:"flex",gap:8,padding:"0 22px 20px",flexWrap:"wrap"}}><button className="primary" style={{margin:0}} disabled={busy||!productId} onClick={save}><Save size={15}/> Lưu bản nháp</button>{selected&&selected.status==="DRAFT"&&<button className="primary" style={{margin:0}} disabled={busy} onClick={()=>transition("submit")}><Send size={15}/> Gửi duyệt</button>}{selected&&selected.status==="IN_REVIEW"&&<button className="primary" style={{margin:0}} disabled={busy} onClick={()=>transition("approve")}><CheckCircle2 size={15}/> Phê duyệt</button>}{selected&&selected.status==="APPROVED"&&<button className="primary" style={{margin:0}} disabled={busy} onClick={()=>transition("publish")}><Globe size={15}/> Xuất bản</button>}</div>
  </div>
 </div>
}
