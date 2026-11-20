# VIB CMS Admin Design System

The React/Vite admin UI now loads `src/vib-design-system.css` after the legacy stylesheet. The new layer intentionally overrides the old styles so existing module workflows remain intact while the whole portal gets a unified visual system.

## Visual direction

- White workspace and navigation surfaces
- VIB blue for navigation, information hierarchy, and selected module tabs
- VIB orange for primary actions and active navigation accents
- Green for successful/secure states
- Soft blue-grey canvas, thin borders, rounded cards, subtle shadows
- Responsive sidebar and form layouts

## Components restyled

- Login screen
- Sidebar, workspace indicator, top bar, user avatar
- Page headings, status indicators, statistics cards
- Panels, tables, forms, buttons, alerts, role cards
- Content Type / Authoring Template / Workflow / Asset Storage / Content Item screens
- Content Item authoring groups, repeater rows, locale inputs
- Tiptap rich-text editor toolbar and editable surface
- Media upload dropzone, asset cards, Media Library selection dialog

The implementation follows a shadcn/ui-inspired enterprise dashboard pattern while retaining the existing React components and API behavior. It does not add a second UI framework or replace the current Vite application.
