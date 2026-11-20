import {useEffect,useRef,useState} from "react";
import {FileUp,Image as ImageIcon,FileText,Trash2,UploadCloud,LoaderCircle,ExternalLink} from "lucide-react";

type Asset= {id?:string;url?:string;name?:string;contentType?:string;size?:number;objectKey?:string};
type Props={value:any;onChange:(value:any)=>void;productId:string;username:string;password:string;multiple?:boolean;accept?:string};
const asAsset=(x:any):Asset=>typeof x==="string"?{url:x,name:x.split("/").pop()||x}:x||{};
export default function AssetField({value,onChange,productId,username,password,multiple=false,accept}:Props){
 const input=useRef<HTMLInputElement>(null),[busy,setBusy]=useState(false),[error,setError]=useState(""),[previews,setPreviews]=useState<Record<string,string>>({});
 const assets:Asset[]=(multiple?(Array.isArray(value)?value:[]):value?[asAsset(value)]:[]).map(asAsset);
 useEffect(()=>{
  let live=true;const urls:string[]=[];
  const load=async()=>{
   const next:Record<string,string>={};
   for(const asset of assets){
    const key=asset.id||asset.url||asset.name||String(Math.random());
    if(asset.url&&asset.url.startsWith("blob:")){next[key]=asset.url;continue}
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
 const remove=(index:number)=>{const next=assets.filter((_,i)=>i!==index);onChange(multiple?next:(next[0]||""))};
 const formatSize=(n:any)=>{const size=Number(n||0);return size<1024?size+" B":size<1048576?(size/1024).toFixed(1)+" KB":(size/1048576).toFixed(1)+" MB"};
 return <div className="asset-field">
  <input ref={input} type="file" hidden multiple={multiple} accept={accept||"image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv"} onChange={e=>upload(e.target.files)}/>
  {assets.length>0&&<div className="asset-grid">{assets.map((asset,index)=>{const key=asset.id||asset.url||asset.name||String(index);const preview=previews[key];const image=(asset.contentType||"").startsWith("image/")||/\.(png|jpe?g|gif|webp|svg)$/i.test(asset.name||asset.url||"");return <div className="asset-card" key={key}>{image&&preview?<img src={preview} alt={asset.name||"Uploaded asset"}/>:<div className="asset-file-icon">{image?<ImageIcon/>:<FileText/>}</div>}<div className="asset-meta"><b title={asset.name}>{asset.name||asset.id||"Asset"}</b><small>{formatSize(asset.size)}</small></div><div className="asset-actions">{asset.url&&<a href={asset.url} target="_blank" rel="noreferrer" title="Open asset"><ExternalLink size={14}/></a>}<button type="button" onClick={()=>remove(index)} title="Remove"><Trash2 size={14}/></button></div></div>})}</div>}
  <button type="button" className="asset-dropzone" disabled={busy} onClick={()=>input.current?.click()} onDragOver={e=>e.preventDefault()} onDrop={e=>{e.preventDefault();upload(e.dataTransfer.files)}}>
   {busy?<LoaderCircle className="spin" size={21}/>:<UploadCloud size={22}/>}<span><b>{busy?"Đang tải file lên…":"Chọn file hoặc kéo thả vào đây"}</b><small>{multiple?"Có thể chọn nhiều file":"Chọn một file"} · Tối đa 20 MB mỗi file</small></span><span className="asset-browse"><FileUp size={15}/> Browse</span>
  </button>
  {error&&<div className="asset-error">{error}</div>}
 </div>;
}
