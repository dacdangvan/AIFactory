import {useEffect,useRef,useState} from "react";
import {FileUp,Image as ImageIcon,FileText,Trash2,UploadCloud,LoaderCircle,ExternalLink,FolderOpen,X,Check} from "lucide-react";

type Asset= {id?:string;url?:string;name?:string;contentType?:string;size?:number;objectKey?:string};
type Props={value:any;onChange:(value:any)=>void;productId:string;username:string;password:string;multiple?:boolean;accept?:string};
const asAsset=(x:any):Asset=>typeof x==="string"?{url:x,name:x.split("/").pop()||x}:x||{};
const formatSize=(n:any)=>{const size=Number(n||0);return size<1024?size+" B":size<1048576?(size/1024).toFixed(1)+" KB":(size/1048576).toFixed(1)+" MB"};
export default function AssetField({value,onChange,productId,username,password,multiple=false,accept}:Props){
 const input=useRef<HTMLInputElement>(null),[busy,setBusy]=useState(false),[error,setError]=useState(""),[previews,setPreviews]=useState<Record<string,string>>({});
 const [library,setLibrary]=useState<Asset[]>([]),[libraryOpen,setLibraryOpen]=useState(false),[libraryBusy,setLibraryBusy]=useState(false);
 const assets:Asset[]=(multiple?(Array.isArray(value)?value:[]):value?[asAsset(value)]:[]).map(asAsset);
 useEffect(()=>{
  let live=true;const urls:string[]=[];
  const load=async()=>{
   const next:Record<string,string>={};
   for(const [index,asset] of assets.entries()){
    const key=asset.id||asset.url||asset.name||String(index);
    if(asset.url&&asset.url.startsWith("http")){next[key]=asset.url;continue}
    if(asset.url){
     try{const response=await fetch(asset.url,{headers:{Authorization:"Basic "+btoa(username+":"+password)}});if(response.ok){const url=URL.createObjectURL(await response.blob());urls.push(url);next[key]=url}}catch{}
    }
   }
   if(live)setPreviews(next);else urls.forEach(URL.revokeObjectURL);
  };
  load();return()=>{live=false;urls.forEach(URL.revokeObjectURL)};
 },[value,username,password]);
 const upload=async(files:FileList|null)=>{
  if(!files?.length)return;setBusy(true);setError("");
  try{
   const uploaded:Asset[]=[];
   for(const file of Array.from(files)){
    const form=new FormData();form.append("file",file);
    const response=await fetch("/api/v1/media"+(productId?"?productId="+encodeURIComponent(productId):""),{method:"POST",headers:{Authorization:"Basic "+btoa(username+":"+password)},body:form});
    const body=await response.json().catch(()=>null);
    if(!response.ok)throw new Error(body?.error?.message||body?.message||"Upload thất bại");
    uploaded.push(body?.data??body);
   }
   onChange(multiple?[...assets,...uploaded]:uploaded[0]);
  }catch(e:any){setError(e.message||"Không thể upload file")}finally{setBusy(false);if(input.current)input.current.value=""}
 };
 const loadLibrary=async()=>{
  setLibraryBusy(true);setError("");
  try{
   const query=new URLSearchParams({limit:"100"});if(productId)query.set("productId",productId);
   const response=await fetch("/api/v1/media?"+query.toString(),{headers:{Authorization:"Basic "+btoa(username+":"+password)}});
   const body=await response.json().catch(()=>null);
   if(!response.ok)throw new Error(body?.error?.message||"Không thể tải Media Library");
   setLibrary(body?.data??body??[]);setLibraryOpen(true);
  }catch(e:any){setError(e.message||"Không thể tải Media Library")}finally{setLibraryBusy(false)}
 };
 const selectLibrary=(asset:Asset)=>{
  if(multiple){if(!assets.some(x=>x.id===asset.id))onChange([...assets,asset])}
  else onChange(asset);
  if(!multiple)setLibraryOpen(false);
 };
 const remove=(index:number)=>{const next=assets.filter((_,i)=>i!==index);onChange(multiple?next:(next[0]||""))};
 return <div className="asset-field">
  <input ref={input} type="file" hidden multiple={multiple} accept={accept||"image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv"} onChange={e=>upload(e.target.files)}/>
  {assets.length>0&&<div className="asset-grid">{assets.map((asset,index)=>{const key=asset.id||asset.url||asset.name||String(index);const preview=previews[key];const image=(asset.contentType||"").startsWith("image/")||/\.(png|jpe?g|gif|webp)$/i.test(asset.name||asset.url||"");return <div className="asset-card" key={key}>{image&&preview?<img src={preview} alt={asset.name||"Uploaded asset"}/>:<div className="asset-file-icon">{image?<ImageIcon/>:<FileText/>}</div>}<div className="asset-meta"><b title={asset.name}>{asset.name||asset.id||"Asset"}</b><small>{formatSize(asset.size)}</small></div><div className="asset-actions">{asset.url&&<a href={asset.url} target="_blank" rel="noreferrer" title="Open asset"><ExternalLink size={14}/></a>}<button type="button" onClick={()=>remove(index)} title="Remove"><Trash2 size={14}/></button></div></div>})}</div>}
  <div className="asset-choices">
   <button type="button" className="asset-dropzone" disabled={busy} onClick={()=>input.current?.click()} onDragOver={e=>e.preventDefault()} onDrop={e=>{e.preventDefault();upload(e.dataTransfer.files)}}>
    {busy?<LoaderCircle className="spin" size={21}/>:<UploadCloud size={22}/>}<span><b>{busy?"Đang tải file lên…":"Chọn file hoặc kéo thả vào đây"}</b><small>{multiple?"Có thể chọn nhiều file":"Chọn một file"} · Tối đa 20 MB mỗi file</small></span><span className="asset-browse"><FileUp size={15}/> Upload</span>
   </button>
   <button type="button" className="secondary asset-library-button" onClick={loadLibrary} disabled={libraryBusy}>{libraryBusy?<LoaderCircle className="spin" size={15}/>:<FolderOpen size={15}/>} Chọn từ Media Library</button>
  </div>
  {error&&<div className="asset-error">{error}</div>}
  {libraryOpen&&<div className="asset-modal-backdrop" onClick={()=>setLibraryOpen(false)}><section className="asset-library-modal" onClick={e=>e.stopPropagation()}><header><div><b>Media Library</b><small>Chọn asset đã upload cho Product hiện tại</small></div><button type="button" className="icon-btn" onClick={()=>setLibraryOpen(false)}><X size={16}/></button></header><div className="asset-library-list">{library.map(asset=><button type="button" className={"asset-library-row "+(assets.some(x=>x.id===asset.id)?"selected":"")} key={asset.id} onClick={()=>selectLibrary(asset)}><span className="asset-library-icon">{(asset.contentType||"").startsWith("image/")?<ImageIcon size={18}/>:<FileText size={18}/>}</span><span className="asset-library-name"><b>{asset.name}</b><small>{asset.contentType} · {formatSize(asset.size)}</small></span>{assets.some(x=>x.id===asset.id)&&<Check size={17}/>}</button>)}{!library.length&&<div className="asset-library-empty">Chưa có asset nào trong thư viện. Hãy upload file mới.</div>}</div><footer><span>{library.length} asset</span><button type="button" className="secondary" onClick={()=>setLibraryOpen(false)}>Đóng</button></footer></section></div>}
 </div>;
}
