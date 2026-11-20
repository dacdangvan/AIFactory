import {Children,isValidElement,useEffect,useMemo,useRef,useState,type ReactNode,type KeyboardEvent} from "react";
import {Check,ChevronDown,Search,X} from "lucide-react";

type Option={value:string;label:string;disabled?:boolean;group?:string};
type Props={
 value?:string;defaultValue?:string;onChange?:(event:{target:{value:string}})=>void;
 children?:ReactNode;disabled?:boolean;className?:string;placeholder?:string;
 name?:string;id?:string;required?:boolean;title?:string; "aria-label"?:string;
};
function collect(nodes:ReactNode,group?:string):Option[]{
 const result:Option[]=[];
 Children.forEach(nodes,node=>{
  if(!isValidElement(node))return;
  if(node.type==="option"){
   const props=node.props as any;
   result.push({value:String(props.value??props.children??""),label:String(props.children??props.value??""),disabled:!!props.disabled,group});
  }else if(node.type==="optgroup"){
   const props=node.props as any;
   result.push(...collect(props.children,String(props.label??"")));
  }
 });
 return result;
}
export default function CustomSelect({value,defaultValue,onChange,children,disabled,className="",placeholder,name,id,required,title,...aria}:Props){
 const [internal,setInternal]=useState(defaultValue??"");
 const selectedValue=value??internal;
 const [open,setOpen]=useState(false);
 const [query,setQuery]=useState("");
 const [active,setActive]=useState(0);
 const root=useRef<HTMLDivElement>(null);
 const search=useRef<HTMLInputElement>(null);
 const options=useMemo(()=>collect(children),[children]);
 const selected=options.find(o=>o.value===selectedValue);
 const filtered=useMemo(()=>options.filter(o=>!query||o.label.toLocaleLowerCase().includes(query.toLocaleLowerCase())||o.group?.toLocaleLowerCase().includes(query.toLocaleLowerCase())),[options,query]);
 const choose=(next:string)=>{if(value===undefined)setInternal(next);onChange?.({target:{value:next}});setOpen(false);setQuery("");};
 useEffect(()=>{if(!open)return;const handler=(event:MouseEvent)=>{if(root.current&&!root.current.contains(event.target as Node)){setOpen(false);setQuery("");}};document.addEventListener("mousedown",handler);return()=>document.removeEventListener("mousedown",handler)},[open]);
 useEffect(()=>{if(open){setActive(Math.max(0,filtered.findIndex(o=>o.value===selectedValue)));requestAnimationFrame(()=>search.current?.focus())}},[open]);
 const move=(delta:number)=>{if(!filtered.length)return;setActive(current=>(current+delta+filtered.length)%filtered.length)};
 const keyDown=(event:KeyboardEvent<HTMLButtonElement>)=>{
  if(event.key==="ArrowDown"){event.preventDefault();if(!open)setOpen(true);else move(1);}
  else if(event.key==="ArrowUp"){event.preventDefault();if(!open)setOpen(true);else move(-1);}
  else if(event.key==="Enter"&&open){event.preventDefault();const option=filtered[active];if(option&&!option.disabled)choose(option.value);}
  else if(event.key==="Escape"){setOpen(false);setQuery("");}
 };
 let lastGroup:string|undefined;
 return <div className={`vib-select-root ${className}`} ref={root}>
  <input type="hidden" name={name} value={selectedValue}/>
  <button id={id} type="button" title={title} aria-label={aria["aria-label"]} aria-haspopup="listbox" aria-expanded={open} aria-required={required} disabled={disabled} className={`vib-select-trigger ${open?"is-open":""} ${!selectedValue?"is-placeholder":""}`} onClick={()=>setOpen(v=>!v)} onKeyDown={keyDown}>
   <span>{selected?.label??placeholder??"Chọn..."}</span><ChevronDown size={17}/>
  </button>
  {open&&<div className="vib-select-popover">
   <div className="vib-select-search"><Search size={15}/><input ref={search} value={query} onChange={e=>{setQuery(e.target.value);setActive(0)}} onKeyDown={e=>{if(e.key==="ArrowDown"){e.preventDefault();move(1)}else if(e.key==="ArrowUp"){e.preventDefault();move(-1)}else if(e.key==="Enter"){e.preventDefault();const o=filtered[active];if(o&&!o.disabled)choose(o.value)}else if(e.key==="Escape"){setOpen(false);setQuery("")}}} placeholder="Tìm kiếm..." /><button type="button" aria-label="Đóng" onClick={()=>{setOpen(false);setQuery("")}}><X size={14}/></button></div>
   <div className="vib-select-options" role="listbox">
    {filtered.length?filtered.map((option,index)=>{const showGroup=option.group&&option.group!==lastGroup;lastGroup=option.group;return <div key={option.value}>{showGroup&&<div className="vib-select-group">{option.group}</div>}<button type="button" role="option" aria-selected={option.value===selectedValue} disabled={option.disabled} className={`vib-select-option ${index===active?"is-active":""} ${option.value===selectedValue?"is-selected":""}`} onMouseEnter={()=>setActive(index)} onClick={()=>choose(option.value)}><span>{option.label}</span>{option.value===selectedValue&&<Check size={15}/>}</button></div>}):<div className="vib-select-empty">Không tìm thấy kết quả</div>}
   </div>
   <div className="vib-select-footer">{filtered.length} lựa chọn</div>
  </div>}
 </div>;
}
