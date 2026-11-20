import {useEffect,useMemo,useState} from "react";
import {Plus,RefreshCw,FilePlus2,Edit3,Trash2,Save,ArrowLeft,Search,Languages,Layers,Send,Undo2,CheckCircle2,XCircle,Globe,Workflow} from "lucide-react";
import RichTextEditor from "./RichTextEditor";
import AssetField from "./AssetField";

type Row=Record<string,any>;
type Props={products:{id:string;code:string;name:string}[];username:string;password:string};
type FieldDef={code:string;label:string;type:string;group?:string;localized?:boolean;cardinality?:string;multiple?:boolean;required?:boolean;options?:{values?:string[];placeholder?:string;defaultValue?:any;minItems?:string|number;maxItems?:string|number};validation?:Record<string,any>;itemFields?:string[];minItems?:number;maxItems?:number};
const api=async(path:string,user:string,pass:string,options:RequestInit={})=>{
 const response=await fetch(path,{...options,headers:{"Content-Type":"application/json","Authorization":"Basic "+btoa(user+":"+pass),...(options.headers||{})}});
 const body=await response.json().catch(()=>null);
 if(!response.ok)throw new Error(body?.error?.message||body?.message||response.status+" "+response.statusText);
 return body?.data??body;
};
const inputValue=(value:any)=>value==null?"":String(value);
const blankValue=(field:FieldDef):any=>{
 if(field.localized)return {"vi-VN":"","en-US":""};
 if(field.type==="repeater"||field.cardinality==="REPEATER"||field.multiple||field.cardinality==="MULTIPLE"||(field.type==="checkbox"&&(field.options?.values||[]).length>0))return [];
 return field.options?.defaultValue??"";
};
const slug=(s:string)=>s.normalize("NFD").replace(/[\u0300-\u036f]/g,"").replace(/đ/g,"d").replace(/Đ/g,"D").toLowerCase().replace(/[^a-z0-9]+/g,"-").replace(/^-|-$/g,"");

const WorkflowIcon=({code}:{code:string})=>{switch(code){case "SUBMIT":case "RESUBMIT":return <Send size={14}/>;case "RECALL":return <Undo2 size={14}/>;case "APPROVE":return <CheckCircle2 size={14}/>;case "REJECT":return <XCircle size={14}/>;case "PUBLISH":return <Globe size={14}/>;case "DELETE":return <Trash2 size={14}/>;case "SAVE":return <Save size={14}/>;default:return <Workflow size={14}/>;}};

export default function ContentItemManagement({products,username,password}:Props){
 const [productId,setProductId]=useState("");
 const [types,setTypes]=useState<Row[]>([]);
 const [typeId,setTypeId]=useState("");
 const [items,setItems]=useState<Row[]>([]);
 const [definition,setDefinition]=useState<Row|null>(null);
 const [fields,setFields]=useState<Record<string,any>>({});
 const [code,setCode]=useState("");
 const [editingId,setEditingId]=useState<string|null>(null);
 const [expectedVersion,setExpectedVersion]=useState(1);
 const [workflowActions,setWorkflowActions]=useState<Row[]>([]);
 const [workflowState,setWorkflowState]=useState<Row|null>(null);
 const [readOnly,setReadOnly]=useState(false);
 const [showEditor,setShowEditor]=useState(false);
 const [keyword,setKeyword]=useState("");
 const [busy,setBusy]=useState(false);
 const [error,setError]=useState("");
 const [notice,setNotice]=useState("");
 const selectedProduct=useMemo(()=>products.find(p=>p.id===productId),[products,productId]);
 const schemaFields=useMemo(()=>((definition?.fieldSchema?.fields||[]) as FieldDef[]),[definition]);
 const repeaterChildCodes=useMemo(()=>new Set(schemaFields.flatMap(f=>f.type==="repeater"||f.cardinality==="REPEATER"?(f.itemFields||[]):[])),[schemaFields]);

 useEffect(()=>{if(!productId&&products.length)setProductId(products[0].id)},[products,productId]);
 const loadTypes=async()=>{
  if(!productId){setTypes([]);setTypeId("");return}
  const result=await api("/api/v1/content-types?productId="+productId,username,password);
  const active=(result||[]).filter((x:Row)=>x.status==="ACTIVE");
  setTypes(active);
  if(!active.some((x:Row)=>x.id===typeId))setTypeId(active[0]?.id||"");
 };
 const loadItems=async()=>{
  if(!productId){setItems([]);return}
  const query=new URLSearchParams({productId,limit:"100"});
  if(typeId)query.set("contentTypeId",typeId);
  if(keyword.trim())query.set("keyword",keyword.trim());
  const result=await api("/api/v1/content-items?"+query.toString(),username,password);
  setItems(result||[]);
 };
 const refresh=async()=>{
  setBusy(true);setError("");
  try{await loadTypes();await loadItems()}catch(e:any){setError(e.message)}finally{setBusy(false)}
 };
 useEffect(()=>{if(productId){setBusy(true);setError("");Promise.all([loadTypes(),loadItems()]).catch((e:any)=>setError(e.message)).finally(()=>setBusy(false))}},[productId]);
 useEffect(()=>{if(productId)loadItems().catch((e:any)=>setError(e.message))},[typeId,keyword]);
 const run=async(fn:()=>Promise<void>,message:string)=>{
  setBusy(true);setError("");setNotice("");
  try{await fn();setNotice(message);await loadItems()}catch(e:any){setError(e.message||"Có lỗi xảy ra")}finally{setBusy(false)}
 };
 const startCreate=async()=>{
  if(!typeId){setError("Content Type chưa được cấu hình");return}
  setBusy(true);setError("");setNotice("");
  try{
   const d=await api("/api/v1/content-items/authoring-definition?contentTypeId="+typeId,username,password);
   setDefinition(d);
   const next:Record<string,any>={};
   for(const f of (d.fieldSchema?.fields||[]) as FieldDef[])next[f.code]=blankValue(f);
   setFields(next);setCode("");setEditingId(null);setExpectedVersion(1);setWorkflowActions([]);setWorkflowState(null);setReadOnly(false);setShowEditor(true);
  }catch(e:any){setError(e.message)}finally{setBusy(false)}
 };
 const startEdit=async(item:Row)=>{
  setBusy(true);setError("");setNotice("");
  try{
   const d=await api("/api/v1/content-items/"+item.id+"/authoring-definition",username,password);
   const detail=await api("/api/v1/content-items/"+item.id,username,password);
   const wf=await api("/api/v1/content-items/"+item.id+"/workflow-actions",username,password);
   setDefinition(d);setFields(detail.dataSnapshot?.fields||{});setCode(detail.code||"");
   setWorkflowActions(wf.availableActions||[]);setWorkflowState(wf);setReadOnly(!wf.canEdit);
   setEditingId(item.id);setExpectedVersion(Number(detail.currentVersion||1));setTypeId(item.contentTypeId);setShowEditor(true);
  }catch(e:any){setError(e.message)}finally{setBusy(false)}
 };
 const persistContent=async():Promise<string>=>{
  if(!typeId)throw new Error("Vui lòng chọn Content Type");
  if(!code.trim())throw new Error("Content Code là bắt buộc");
  const payload={code:code.trim(),fields,expectedVersion};
  if(editingId){await api("/api/v1/content-items/"+editingId,username,password,{method:"PUT",body:JSON.stringify(payload)});setExpectedVersion(expectedVersion+1);return editingId;}
  const created=await api("/api/v1/content-items?productId="+productId+"&contentTypeId="+typeId,username,password,{method:"POST",body:JSON.stringify(payload)});
  return String(created.id);
 };
 const performItemAction=async(id:string,action:string,comment?:string)=>{
  const wf=await api("/api/v1/content-items/"+id+"/workflow-actions",username,password);
  const selected=(wf.availableActions||[]).find((x:Row)=>x.code===action);
  if(!selected)throw new Error("Bạn không có quyền thực hiện thao tác "+action+" ở trạng thái hiện tại");
  return api("/api/v1/content-items/"+id+"/workflow-actions",username,password,{method:"POST",body:JSON.stringify({action,comment,expectedLockVersion:wf.lockVersion})});
 };
 const finishWorkflow=()=>{setShowEditor(false);setEditingId(null);setDefinition(null);setWorkflowActions([]);setWorkflowState(null);};
 const saveDraft=()=>run(async()=>{
  if(readOnly)throw new Error("Content Item đang ở trạng thái chỉ xem");
  const id=await persistContent();
  await performItemAction(id,"SAVE");
  finishWorkflow();
 },"Đã lưu nháp");
 const submitForReview=()=>run(async()=>{
  if(readOnly)throw new Error("Bạn không có quyền chỉnh sửa nội dung này");
  const id=await persistContent();
  await performItemAction(id,"SUBMIT");
  finishWorkflow();
 },"Đã gửi nội dung để duyệt");
 const executeAction=(action:Row)=>run(async()=>{
  if(!editingId)throw new Error("Content Item chưa được lưu");
  let comment:string|undefined;
  if(action.code==="REJECT"){comment=window.prompt("Nhập lý do từ chối (bắt buộc)")||"";if(!comment.trim())throw new Error("Vui lòng nhập lý do từ chối");}
  if(action.code==="DELETE"&&!window.confirm("Xóa Content Item "+code+"?"))throw new Error("Đã hủy thao tác xóa");
  if(action.code==="APPROVE"&&!window.confirm("Xác nhận phê duyệt Content Item "+code+"?"))throw new Error("Đã hủy thao tác phê duyệt");
  await api("/api/v1/content-items/"+editingId+"/workflow-actions",username,password,{method:"POST",body:JSON.stringify({action:action.code,comment,expectedLockVersion:workflowState?.lockVersion})});
  finishWorkflow();
 },action.code==="DELETE"?"Đã xóa Content Item":action.code==="REJECT"?"Đã từ chối nội dung":action.code==="APPROVE"?"Đã phê duyệt nội dung":"Đã thực hiện "+action.name);
 const archive=async(item:Row)=>{
  if(!window.confirm("Archive Content Item "+item.code+"?"))return;
  await run(async()=>{await api("/api/v1/content-items/"+item.id,username,password,{method:"DELETE"})},"Đã archive Content Item");
 };
 const setField=(field:FieldDef,value:any)=>setFields(old=>({...old,[field.code]:value}));
 const renderControl=(field:FieldDef,value:any,onChange:(v:any)=>void)=>{
  const common={required:!!field.required,placeholder:field.options?.placeholder||""};
  const type=field.type||"text";
  if(type==="richtext")return <RichTextEditor value={inputValue(value)} onChange={onChange} placeholder={field.options?.placeholder||"Nhập nội dung..."}/>;
  if(["image","image_gallery","file","file_gallery","video","audio"].includes(type))return <AssetField value={value} onChange={onChange} productId={productId} username={username} password={password} multiple={["image_gallery","file_gallery"].includes(type)||!!field.multiple} accept={type.startsWith("image")?"image/jpeg,image/png,image/gif,image/webp,.jpg,.jpeg,.png,.gif,.webp":type==="video"?"video/mp4,video/webm,.mp4,.webm":type==="audio"?"audio/mpeg,audio/mp4,audio/wav,audio/ogg,.mp3,.m4a,.wav,.ogg":undefined}/>;
  if(["textarea","markdown","html","code"].includes(type))return <textarea rows={type==="textarea"?4:8} {...common} value={inputValue(value)} onChange={e=>onChange(e.target.value)}/>;
  if(["select","radio"].includes(type))return <select {...common} value={inputValue(value)} onChange={e=>onChange(e.target.value)}><option value="">-- Chọn --</option>{(field.options?.values||[]).map((x:string)=><option key={x} value={x}>{x}</option>)}</select>;
  if(type==="checkbox"&&(field.options?.values||[]).length)return <div className="option-list">{(field.options?.values||[]).map((x:string)=><label key={x}><input type="checkbox" checked={Array.isArray(value)&&value.includes(x)} onChange={e=>onChange(e.target.checked?[...(Array.isArray(value)?value:[]),x]:(value||[]).filter((v:string)=>v!==x))}/>{x}</label>)}</div>;
  if(type==="checkbox"||type==="toggle")return <label className="field-inline"><input type="checkbox" checked={!!value} onChange={e=>onChange(e.target.checked)}/> {type==="toggle"?"Bật":"Có"}</label>;
  if(type==="multiselect")return <div className="option-list">{(field.options?.values||[]).map((x:string)=><label key={x}><input type="checkbox" checked={Array.isArray(value)&&value.includes(x)} onChange={e=>onChange(e.target.checked?[...(Array.isArray(value)?value:[]),x]:(value||[]).filter((v:string)=>v!==x))}/>{x}</label>)}</div>;
  if(type==="number"||type==="currency"||type==="percentage"||type==="slider")return <input type="number" {...common} min={field.validation?.min} max={field.validation?.max} value={inputValue(value)} onChange={e=>onChange(e.target.value===""?"":Number(e.target.value))}/>;
  if(type==="date"||type==="time"||type==="datetime")return <input type={type==="datetime"?"datetime-local":type} {...common} value={inputValue(value)} onChange={e=>onChange(e.target.value)}/>;
  if(type==="email"||type==="url"||type==="phone")return <input type={type==="phone"?"tel":type} {...common} value={inputValue(value)} onChange={e=>onChange(e.target.value)}/>;

  return <input type="text" {...common} minLength={field.validation?.minLength} maxLength={field.validation?.maxLength} pattern={field.validation?.pattern||undefined} value={inputValue(value)} onChange={e=>onChange(type==="slug"?slug(e.target.value):e.target.value)}/>;
 };
 const renderField=(field:FieldDef, value:any, onChange:(v:any)=>void, keyPrefix="")=>{
  const id=keyPrefix+field.code;
  if(field.type==="repeater"||field.cardinality==="REPEATER"){
   const rows=Array.isArray(value)?value:[];
   const children=schemaFields.filter(f=>(field.itemFields||[]).includes(f.code));
   return <div className="repeater-list">{rows.map((row:any,index:number)=><div className="repeater-row" key={index}><div className="repeater-head"><b>{field.label} #{index+1}</b><button type="button" className="danger-ghost" onClick={()=>onChange(rows.filter((_,i)=>i!==index))}><Trash2 size={14}/> Xóa</button></div>{children.map(child=><div className="field" key={child.code}><span>{child.label}{child.required?" *":""}</span>{child.localized?<div className="locale-grid">{["vi-VN","en-US"].map(locale=><label key={locale}><small>{locale}</small>{renderControl(child,row?.[child.code]?.[locale]??"",v=>onChange(rows.map((r,i)=>i===index?{...r,[child.code]:{...(r?.[child.code]||{}),[locale]:v}}:r)))}</label>)}</div>:renderControl(child,row?.[child.code],v=>onChange(rows.map((r,i)=>i===index?{...r,[child.code]:v}:r)))}</div>)}</div>)}<button type="button" className="secondary" onClick={()=>onChange([...rows,Object.fromEntries(children.map(f=>[f.code,blankValue(f)]))])} disabled={!!Number(field.options?.maxItems)&&rows.length>=Number(field.options?.maxItems)}><Plus size={14}/> Thêm {field.label.toLowerCase()}</button></div>;
  }
  if(field.localized)return <div className="locale-grid">{["vi-VN","en-US"].map(locale=><label key={locale} className="field"><span><Languages size={13}/> {locale}</span>{renderControl(field,value?.[locale]??"",v=>onChange({...((value&&typeof value==="object")?value:{}),[locale]:v}))}</label>)}</div>;
  if(["image","image_gallery","file","file_gallery","video","audio"].includes(field.type))return renderControl(field,value,onChange);
  if(field.multiple||field.cardinality==="MULTIPLE"){
   const values=Array.isArray(value)?value:[];
   return <div className="repeater-list">{values.map((v:any,i:number)=><div key={i} style={{display:"flex",gap:8}}>{renderControl(field,v,next=>onChange(values.map((x,j)=>j===i?next:x)))}<button type="button" className="icon-btn" onClick={()=>onChange(values.filter((_,j)=>j!==i))}><Trash2 size={14}/></button></div>)}<button type="button" className="secondary" onClick={()=>onChange([...values,""])}><Plus size={14}/> Thêm giá trị</button></div>;
  }
  return renderControl(field,value,onChange);
 };

 return <div className="content-items">
  <div className="panel">
   <div className="panel-head"><div><h2>Content Items</h2><p>Nhập và quản lý nội dung theo Authoring Template được gán cho từng Content Type.</p></div><div style={{display:"flex",gap:8}}><select value={productId} onChange={e=>setProductId(e.target.value)}><option value="">Chọn Product</option>{products.map(p=><option key={p.id} value={p.id}>{p.code} · {p.name}</option>)}</select><button className="icon-btn" onClick={refresh} title="Refresh"><RefreshCw size={16}/></button></div></div>
   {error&&<div className="alert error">{error}</div>}{notice&&<div className="alert success">{notice}</div>}
   {!showEditor?<><div className="items-toolbar"><label className="field"><span>Content Type</span><select value={typeId} onChange={e=>setTypeId(e.target.value)}><option value="">Tất cả Content Type</option>{types.map(t=><option key={t.id} value={t.id}>{t.name} ({t.code})</option>)}</select></label><label className="search"><Search size={16}/><input placeholder="Tìm Content Code..." value={keyword} onChange={e=>setKeyword(e.target.value)}/></label><button className="primary" disabled={!typeId||busy} onClick={startCreate}><FilePlus2 size={15}/> Tạo Content Item</button></div>
    <div className="table-wrap"><table><thead><tr><th>CONTENT CODE</th><th>CONTENT TYPE</th><th>WORKFLOW STATE</th><th>VERSION</th><th>UPDATED</th><th>ACTIONS</th></tr></thead><tbody>{items.map(item=><tr key={item.id}><td><b>{item.code}</b><small>{item.id}</small></td><td>{item.contentTypeName}</td><td><span className="tag">{item.workflowStateName||item.workflowState||"—"}</span></td><td>v{item.currentVersion}</td><td>{item.updatedAt?new Date(item.updatedAt).toLocaleString("vi-VN"):"—"}</td><td><div style={{display:"flex",gap:6}}><button className="icon-btn" title="Workflow / Edit" onClick={()=>startEdit(item)}><Workflow size={15}/></button><button className="icon-btn" title="Delete" onClick={()=>archive(item)}><Trash2 size={15}/></button></div></td></tr>)}{!items.length&&<tr><td colSpan={6} style={{textAlign:"center",padding:28,color:"var(--muted)"}}>Chưa có Content Item nào</td></tr>}</tbody></table></div></>:<div className="item-editor">
    <div className="editor-head"><div><button className="secondary" onClick={()=>setShowEditor(false)}><ArrowLeft size={14}/> Quay lại danh sách</button><h2>{editingId?(readOnly?"Xem / xử lý Content Item":"Chỉnh sửa Content Item"):"Tạo Content Item mới"}</h2><p>{selectedProduct?.name} · {definition?.code} · {definition?.templateName} (v{definition?.templateVersion})</p></div><span className="tag blue">{editingId?(workflowState?.stateName||workflowState?.stateCode||"WORKFLOW"):"NEW"}</span></div>
    {editingId&&<div className="workflow-action-panel"><div className="workflow-current"><span>TRẠNG THÁI WORKFLOW</span><b>{workflowState?.stateName||workflowState?.stateCode||"—"}</b><small>{readOnly?"Chế độ chỉ xem · Không có quyền lưu":"Có thể thao tác theo quyền Workflow của bạn"}</small></div><div className="workflow-action-buttons">{workflowActions.filter(a=>a.code!=="SAVE").map(action=><button key={action.code} type="button" className={action.code==="REJECT"||action.code==="DELETE"?"secondary workflow-danger":action.code==="APPROVE"||action.code==="PUBLISH"?"primary workflow-success":"secondary"} onClick={()=>executeAction(action)} disabled={busy}><WorkflowIcon code={action.code}/>{action.name}</button>)}</div></div>}
    <div className={readOnly?"workflow-readonly":""}><div className="form-grid"><label className="field"><span>Content Code *</span><input value={code} disabled={!!editingId} placeholder="VD: HOME_LOAN_OVERVIEW" onChange={e=>setCode(e.target.value)}/></label><label className="field"><span>Content Type</span><input readOnly value={definition?.name||""}/></label></div>
    {(definition?.fieldSchema?.groups||[]).map((group:Row)=><section className="authoring-group" key={group.code}><div className="group-title"><Layers size={17}/><h3>{group.name}</h3></div><div className="form-grid">{schemaFields.filter(f=>f.group===group.code&&!repeaterChildCodes.has(f.code)).map(field=><div className="field" key={field.code} style={{gridColumn:["textarea","richtext","markdown","html","code","repeater","image","image_gallery","file","file_gallery","video","audio"].includes(field.type)?"1/-1":undefined}}><span>{field.label}{field.required?" *":""} <small>({field.code} · {field.type})</small></span>{renderField(field,fields[field.code]??blankValue(field),v=>setField(field,v))}</div>)}</div></section>)}
    {schemaFields.filter(f=>!repeaterChildCodes.has(f.code)&&!(definition?.fieldSchema?.groups||[]).some((g:Row)=>g.code===f.group)).map(field=><div className="field" key={field.code}><span>{field.label}{field.required?" *":""}</span>{renderField(field,fields[field.code]??blankValue(field),v=>setField(field,v))}</div>)}
    </div><div className="editor-footer"><span>Content được lưu theo snapshot version; mỗi lần lưu tạo một version mới.</span><div className="editor-workflow-buttons">{(!editingId||workflowActions.some(a=>a.code==="SAVE"))&&<button className="secondary" disabled={busy||readOnly} onClick={saveDraft}><Save size={15}/> Lưu nháp</button>}{(!editingId||workflowActions.some(a=>a.code==="SUBMIT"))&&<button className="primary" disabled={busy||readOnly} onClick={submitForReview}><Send size={15}/> Gửi duyệt</button>}</div></div>
   </div>}
  </div>
 </div>;
}
