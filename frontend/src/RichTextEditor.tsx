import {useEffect} from "react";
import {EditorContent,useEditor,useEditorState} from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import TextAlign from "@tiptap/extension-text-align";
import Image from "@tiptap/extension-image";
import Placeholder from "@tiptap/extension-placeholder";
import {Bold,Italic,Underline as UnderlineIcon,Strikethrough,Heading1,Heading2,List,ListOrdered,Quote,Link2,Image as ImageIcon,AlignLeft,AlignCenter,AlignRight,Undo2,Redo2,Minus} from "lucide-react";

type Props={value:string;onChange:(value:string)=>void;placeholder?:string};
export default function RichTextEditor({value,onChange,placeholder="Bắt đầu soạn nội dung..."}:Props){
 const editor=useEditor({
  extensions:[
   StarterKit.configure({heading:{levels:[1,2,3]}}),
   TextAlign.configure({types:["heading","paragraph"]}),
   Image.configure({inline:false,allowBase64:false}),
   Placeholder.configure({placeholder})
  ],
  content:value||"",
  onUpdate:({editor})=>onChange(editor.getHTML())
 });
 const editorState=useEditorState({editor,selector:({editor})=>({
  bold:!!editor?.isActive("bold"),italic:!!editor?.isActive("italic"),underline:!!editor?.isActive("underline"),
  strike:!!editor?.isActive("strike"),h1:!!editor?.isActive("heading",{level:1}),h2:!!editor?.isActive("heading",{level:2}),
  bullet:!!editor?.isActive("bulletList"),ordered:!!editor?.isActive("orderedList"),quote:!!editor?.isActive("blockquote"),
  left:!!editor?.isActive({textAlign:"left"}),center:!!editor?.isActive({textAlign:"center"}),right:!!editor?.isActive({textAlign:"right"}),link:!!editor?.isActive("link")
 })});
 useEffect(()=>{if(editor&&value!==editor.getHTML())editor.commands.setContent(value||"",{emitUpdate:false})},[editor,value]);
 if(!editor)return <div className="rte-loading">Đang tải trình soạn thảo…</div>;
 const action=(label:string,active:boolean,run:()=>void,Icon:any)=> <button type="button" title={label} aria-label={label} className={active?"rte-tool active":"rte-tool"} onClick={run}><Icon size={16}/></button>;
 const link=()=>{const previous=editor.getAttributes("link").href||"";const url=window.prompt("Nhập URL liên kết",previous);if(url===null)return;if(!url.trim())editor.chain().focus().extendMarkRange("link").unsetLink().run();else editor.chain().focus().extendMarkRange("link").setLink({href:url.trim()}).run()};
 const image=()=>{const url=window.prompt("Nhập URL ảnh");if(url?.trim())editor.chain().focus().setImage({src:url.trim(),alt:""}).run()};
 return <div className="rte-shell">
  <div className="rte-toolbar">
   {action("Bold",(editorState?.bold ?? false),()=>editor.chain().focus().toggleBold().run(),Bold)}
   {action("Italic",(editorState?.italic ?? false),()=>editor.chain().focus().toggleItalic().run(),Italic)}
   {action("Underline",(editorState?.underline ?? false),()=>editor.chain().focus().toggleUnderline().run(),UnderlineIcon)}
   {action("Strikethrough",(editorState?.strike ?? false),()=>editor.chain().focus().toggleStrike().run(),Strikethrough)}
   <span className="rte-separator"/>
   {action("Heading 1",(editorState?.h1 ?? false),()=>editor.chain().focus().toggleHeading({level:1}).run(),Heading1)}
   {action("Heading 2",(editorState?.h2 ?? false),()=>editor.chain().focus().toggleHeading({level:2}).run(),Heading2)}
   {action("Bullet list",(editorState?.bullet ?? false),()=>editor.chain().focus().toggleBulletList().run(),List)}
   {action("Numbered list",(editorState?.ordered ?? false),()=>editor.chain().focus().toggleOrderedList().run(),ListOrdered)}
   {action("Quote",(editorState?.quote ?? false),()=>editor.chain().focus().toggleBlockquote().run(),Quote)}
   <span className="rte-separator"/>
   {action("Align left",(editorState?.left ?? false),()=>editor.chain().focus().setTextAlign("left").run(),AlignLeft)}
   {action("Align center",(editorState?.center ?? false),()=>editor.chain().focus().setTextAlign("center").run(),AlignCenter)}
   {action("Align right",(editorState?.right ?? false),()=>editor.chain().focus().setTextAlign("right").run(),AlignRight)}
   {action("Insert link",(editorState?.link ?? false),link,Link2)}
   {action("Insert image URL",false,image,ImageIcon)}
   {action("Horizontal line",false,()=>editor.chain().focus().setHorizontalRule().run(),Minus)}
   <span className="rte-spacer"/>
   {action("Undo",false,()=>editor.chain().focus().undo().run(),Undo2)}
   {action("Redo",false,()=>editor.chain().focus().redo().run(),Redo2)}
  </div>
  <EditorContent editor={editor} className="rte-content"/>
  <div className="rte-foot"><span>Định dạng nội dung bằng thanh công cụ</span><span>Rich text · HTML</span></div>
 </div>;
}
