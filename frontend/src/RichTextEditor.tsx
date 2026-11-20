import {useEffect} from "react";
import {EditorContent,useEditor} from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Link from "@tiptap/extension-link";
import Underline from "@tiptap/extension-underline";
import TextAlign from "@tiptap/extension-text-align";
import Image from "@tiptap/extension-image";
import Placeholder from "@tiptap/extension-placeholder";
import {Bold,Italic,Underline as UnderlineIcon,Strikethrough,Heading1,Heading2,List,ListOrdered,Quote,Link2,Image as ImageIcon,AlignLeft,AlignCenter,AlignRight,Undo2,Redo2,Minus} from "lucide-react";

type Props={value:string;onChange:(value:string)=>void;placeholder?:string};
export default function RichTextEditor({value,onChange,placeholder="Bắt đầu soạn nội dung..."}:Props){
 const editor=useEditor({
  extensions:[
   StarterKit.configure({heading:{levels:[1,2,3]}}),
   Underline,
   Link.configure({openOnClick:false,autolink:true,linkOnPaste:true}),
   TextAlign.configure({types:["heading","paragraph"]}),
   Image.configure({inline:false,allowBase64:false}),
   Placeholder.configure({placeholder})
  ],
  content:value||"",
  onUpdate:({editor})=>onChange(editor.getHTML())
 });
 useEffect(()=>{if(editor&&value!==editor.getHTML())editor.commands.setContent(value||"",{emitUpdate:false})},[editor,value]);
 if(!editor)return <div className="rte-loading">Đang tải trình soạn thảo…</div>;
 const action=(label:string,active:boolean,run:()=>void,Icon:any)=> <button type="button" title={label} aria-label={label} className={active?"rte-tool active":"rte-tool"} onClick={run}><Icon size={16}/></button>;
 const link=()=>{const previous=editor.getAttributes("link").href||"";const url=window.prompt("Nhập URL liên kết",previous);if(url===null)return;if(!url.trim())editor.chain().focus().extendMarkRange("link").unsetLink().run();else editor.chain().focus().extendMarkRange("link").setLink({href:url.trim()}).run()};
 const image=()=>{const url=window.prompt("Nhập URL ảnh");if(url?.trim())editor.chain().focus().setImage({src:url.trim(),alt:""}).run()};
 return <div className="rte-shell">
  <div className="rte-toolbar">
   {action("Bold",editor.isActive("bold"),()=>editor.chain().focus().toggleBold().run(),Bold)}
   {action("Italic",editor.isActive("italic"),()=>editor.chain().focus().toggleItalic().run(),Italic)}
   {action("Underline",editor.isActive("underline"),()=>editor.chain().focus().toggleUnderline().run(),UnderlineIcon)}
   {action("Strikethrough",editor.isActive("strike"),()=>editor.chain().focus().toggleStrike().run(),Strikethrough)}
   <span className="rte-separator"/>
   {action("Heading 1",editor.isActive("heading",{level:1}),()=>editor.chain().focus().toggleHeading({level:1}).run(),Heading1)}
   {action("Heading 2",editor.isActive("heading",{level:2}),()=>editor.chain().focus().toggleHeading({level:2}).run(),Heading2)}
   {action("Bullet list",editor.isActive("bulletList"),()=>editor.chain().focus().toggleBulletList().run(),List)}
   {action("Numbered list",editor.isActive("orderedList"),()=>editor.chain().focus().toggleOrderedList().run(),ListOrdered)}
   {action("Quote",editor.isActive("blockquote"),()=>editor.chain().focus().toggleBlockquote().run(),Quote)}
   <span className="rte-separator"/>
   {action("Align left",editor.isActive({textAlign:"left"}),()=>editor.chain().focus().setTextAlign("left").run(),AlignLeft)}
   {action("Align center",editor.isActive({textAlign:"center"}),()=>editor.chain().focus().setTextAlign("center").run(),AlignCenter)}
   {action("Align right",editor.isActive({textAlign:"right"}),()=>editor.chain().focus().setTextAlign("right").run(),AlignRight)}
   {action("Insert link",editor.isActive("link"),link,Link2)}
   {action("Insert image URL",false,image,ImageIcon)}
   {action("Horizontal line",false,()=>editor.chain().focus().setHorizontalRule().run(),Minus)}
   <span className="rte-spacer"/>
   {action("Undo",false,()=>editor.chain().focus().undo().run(),Undo2)}
   {action("Redo",false,()=>editor.chain().focus().redo().run(),Redo2)}
  </div>
  <EditorContent editor={editor} className="rte-content"/>
  <div className="rte-foot"><span>Định dạng nội dung bằng thanh công cụ</span><span>{editor.storage.characterCount?.characters?.()??""}</span></div>
 </div>;
}
