import{d as le,I as re,h as D,g as ie,j as U,ac as w,cs as N,ct as ce,au as pe,k as _e,av as F,o as d,c as u,a as e,t as a,m as s,x as g,cv as A,O as h,P as b,y as x,z as ee,p as J,q as te,e as V,V as L,ao as ve,l as me,C as fe,cw as ge,cx as $e,Z as S,S as he,ap as ae,U as oe,w as ne,ad as ye,a3 as be}from"./index-R8dLcqG_.js";import{T as v,a as k,_ as ke,A as we}from"./question-mark-rounded-BC93NM4N.js";import{u as Ce,a as Te}from"./vee-validate.esm-vpr6zxQb.js";const Ne={slot:"headline"},Ie={slot:"content"},Ee={class:"row mb-3"},Fe={class:"col-md-3 col-form-label"},Ae={class:"col-md-9"},Ve=["value"],De={key:0,class:"input-group"},Re=["placeholder"],Me={class:"inner"},Oe={class:"help-block"},qe={value:""},Se=["value"],Ue={key:2,class:"invalid-feedback"},Je={class:"row mb-3"},Le={class:"col-md-3 col-form-label"},je={class:"col-md-9"},Be=["value"],Pe={class:"row mb-3"},ze={class:"col-md-3 col-form-label"},Qe={class:"col-md-9"},Ze={value:"all"},Ge=["value"],He=["value"],Ke={class:"row mb-3"},We={class:"col-md-3 col-form-label"},Xe={class:"col-md-9"},Ye={slot:"actions"},xe=["disabled"],et={key:0,indeterminate:"",slot:"icon"},se=le({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(y){var j,B,P,z,Q,Z,G,H,K;const{handleSubmit:I}=Ce(),r=re({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),c=D(v.INTERNET),R=Object.values(v).filter(n=>[v.IP,v.NET,v.REMOTE_PORT,v.INTERNET].includes(n)),{t:C}=ie(),m=y,{mutate:M,loading:E,onDone:O}=U({document:w`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `,options:{update:(n,i)=>{ce(n,i.data.createConfig,w`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${N}
        `)}}}),{mutate:q,loading:o,onDone:T}=U({document:w`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `}),{value:p,resetField:f,errorMessage:l}=Te("inputValue",pe().test("required",n=>"valid.required",n=>!k.hasInput(c.value)||!!n).test("target-value",n=>"invalid_value",n=>k.isValid(c.value,n??""))),_=(j=m.data)==null?void 0:j.data;c.value=((P=(B=m.data)==null?void 0:B.target)==null?void 0:P.type)??v.INTERNET,p.value=((Q=(z=m.data)==null?void 0:z.target)==null?void 0:Q.value)??"",r.apply_to=((G=(Z=m.data)==null?void 0:Z.applyTo)==null?void 0:G.toValue())??"all",r.if_name=(_==null?void 0:_.if_name)??((K=(H=m.networks)==null?void 0:H[0])==null?void 0:K.ifName)??"",r.notes=(_==null?void 0:_.notes)??"",r.is_enabled=(_==null?void 0:_.is_enabled)??!0,_||f(),_e(c,(n,i)=>{(n===v.INTERFACE||i===v.INTERFACE)&&(p.value="")});const $=I(()=>{const n=new k;n.type=c.value,n.value=p.value??"",r.target=n.toValue(),m.data?q({id:m.data.id,input:{group:"route",value:JSON.stringify(r)}}):M({input:{group:"route",value:JSON.stringify(r)}})});return O(()=>{F()}),T(()=>{F()}),(n,i)=>{var W,X,Y;const de=ke,ue=ve;return d(),u("md-dialog",null,[e("div",Ne,a(s(_)?n.$t("edit"):n.$t("create")),1),e("div",Ie,[e("div",Ee,[e("label",Fe,a(n.$t("traffic_to")),1),e("div",Ae,[g(e("select",{class:"form-select","onUpdate:modelValue":i[0]||(i[0]=t=>c.value=t)},[(d(!0),u(h,null,b(s(R),t=>(d(),u("option",{key:t,value:t},a(n.$t(`target_type.${t}`)),9,Ve))),128))],512),[[A,c.value]]),s(k).hasInput(c.value)?(d(),u("div",De,[g(e("input",{type:"text",class:"form-control","onUpdate:modelValue":i[1]||(i[1]=t=>ee(p)?p.value=t:null),placeholder:n.$t("for_example")+" "+s(k).hint(c.value)},null,8,Re),[[x,s(p)]]),J(ue,{class:"input-group-text"},{content:te(()=>[e("pre",Oe,a(n.$t(`examples_${c.value}`)),1)]),default:te(()=>[e("span",Me,[J(de)])]),_:1})])):V("",!0),c.value===s(v).INTERFACE?g((d(),u("select",{key:1,class:"form-select","onUpdate:modelValue":i[2]||(i[2]=t=>ee(p)?p.value=t:null)},[e("option",qe,a(n.$t("all_local_networks")),1),(d(!0),u(h,null,b((W=y.networks)==null?void 0:W.filter(t=>t.type!=="wan"),t=>(d(),u("option",{value:t.ifName},a(t.name),9,Se))),256))],512)),[[A,s(p)]]):V("",!0),s(l)?(d(),u("div",Ue,a(s(l)?n.$t(s(l)):""),1)):V("",!0)])]),e("div",Je,[e("label",Le,a(s(C)("route_via")),1),e("div",je,[g(e("select",{class:"form-select","onUpdate:modelValue":i[3]||(i[3]=t=>r.if_name=t)},[(d(!0),u(h,null,b((X=y.networks)==null?void 0:X.filter(t=>["wan","vpn"].includes(t.type)),t=>(d(),u("option",{key:t.ifName,value:t.ifName},a(t.name),9,Be))),128))],512),[[A,r.if_name]])])]),e("div",Pe,[e("label",ze,a(s(C)("apply_to")),1),e("div",Qe,[g(e("select",{class:"form-select","onUpdate:modelValue":i[4]||(i[4]=t=>r.apply_to=t)},[e("option",Ze,a(n.$t("all_devices")),1),(d(!0),u(h,null,b((Y=y.networks)==null?void 0:Y.filter(t=>!["wan","vpn"].includes(t.type)),t=>(d(),u("option",{key:t.ifName,value:"iface:"+t.ifName},a(t.name),9,Ge))),128)),(d(!0),u(h,null,b(y.devices,t=>(d(),u("option",{value:"mac:"+t.mac},a(t.name),9,He))),256))],512),[[A,r.apply_to]])])]),e("div",Ke,[e("label",We,a(s(C)("notes")),1),e("div",Xe,[g(e("textarea",{class:"form-control","onUpdate:modelValue":i[5]||(i[5]=t=>r.notes=t),rows:"3"},null,512),[[x,r.notes]])])])]),e("div",Ye,[e("md-outlined-button",{value:"cancel",onClick:i[6]||(i[6]=(...t)=>s(F)&&s(F)(...t))},a(n.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:s(E)||s(o),onClick:i[7]||(i[7]=(...t)=>s($)&&s($)(...t)),autofocus:""},[s(E)||s(o)?(d(),u("md-circular-progress",et)):V("",!0),L(" "+a(n.$t("save")),1)],8,xe)])])}}}),tt={class:"top-app-bar"},at={class:"title"},ot={class:"actions"},nt={class:"table-responsive"},st={class:"table"},lt=e("th",null,"ID",-1),it={class:"actions two"},dt={class:"form-check"},ut=["disabled","onChange","checked"],rt={class:"nowrap"},ct={class:"nowrap"},pt={class:"actions two"},_t=["onClick"],vt=["onClick"],$t=le({__name:"RoutesView",setup(y){const I=D([]),r=D([]),c=D([]),{t:R}=ie();me({handle:(o,T)=>{T?fe(R(T),"error"):(I.value=o.configs.filter(p=>p.group==="route").map(p=>{const f=JSON.parse(p.value),l=new we;l.parse(f.apply_to);const _=new k;return _.parse(f.target),{id:p.id,createdAt:p.createdAt,updatedAt:p.updatedAt,data:f,applyTo:l,target:_}}),r.value=[...o.devices],c.value=[...o.networks])},document:w`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${ge}
    ${N}
    ${$e}
  `});function C(o){S(ye,{id:o.id,name:o.id,gql:w`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function m(o){S(se,{data:o,devices:r,networks:c})}function M(){S(se,{data:null,devices:r,networks:c})}const{mutate:E,loading:O}=U({document:w`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${N}
  `});function q(o){E({id:o.id,input:{group:"route",value:JSON.stringify(o.data)}})}return(o,T)=>{const p=be,f=he("tooltip");return d(),u(h,null,[e("div",tt,[e("div",at,a(o.$t("page_title.routes")),1),e("div",ot,[e("button",{type:"button",class:"btn",onClick:M},a(o.$t("create")),1)])]),e("div",nt,[e("table",st,[e("thead",null,[e("tr",null,[lt,e("th",null,a(o.$t("apply_to")),1),e("th",null,a(o.$t("description")),1),e("th",null,a(o.$t("notes")),1),e("th",null,a(o.$t("enabled")),1),e("th",null,a(o.$t("created_at")),1),e("th",null,a(o.$t("updated_at")),1),e("th",it,a(o.$t("actions")),1)])]),e("tbody",null,[(d(!0),u(h,null,b(I.value,l=>{var _;return d(),u("tr",{key:l.id},[e("td",null,[J(p,{id:l.id,raw:l.data},null,8,["id","raw"])]),e("td",null,a(l.applyTo.getText(o.$t,r.value,c.value)),1),e("td",null,a(o.$t("route_description",{if_name:((_=c.value.find($=>$.ifName==l.data.if_name))==null?void 0:_.name)??l.data.if_name,target:l.target.getText(o.$t,c.value)})),1),e("td",null,a(l.notes),1),e("td",null,[e("div",dt,[e("md-checkbox",{"touch-target":"wrapper",disabled:s(O),onChange:$=>q(l),checked:l.data.is_enabled},null,40,ut)])]),e("td",rt,[g((d(),u("time",null,[L(a(s(oe)(l.createdAt)),1)])),[[f,s(ae)(l.createdAt)]])]),e("td",ct,[g((d(),u("time",null,[L(a(s(oe)(l.updatedAt)),1)])),[[f,s(ae)(l.updatedAt)]])]),e("td",pt,[e("a",{href:"#",class:"v-link",onClick:ne($=>m(l),["prevent"])},a(o.$t("edit")),9,_t),e("a",{href:"#",class:"v-link",onClick:ne($=>C(l),["prevent"])},a(o.$t("delete")),9,vt)])])}),128))])])])],64)}}});export{$t as default};
